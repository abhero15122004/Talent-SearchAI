package com.talentmatch.resume.service;

import com.talentmatch.common.exception.BadRequestException;
import com.talentmatch.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final StorageProperties storageProperties;
    private Path rootStoragePath;

    public StorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() {
        try {
            this.rootStoragePath = Paths.get(storageProperties.getStorageDir()).toAbsolutePath().normalize();
            if (!Files.exists(rootStoragePath)) {
                Files.createDirectories(rootStoragePath);
                log.info("Created resume storage directory at {}", rootStoragePath);
            } else {
                log.info("Using existing resume storage directory at {}", rootStoragePath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize storage directory: " + storageProperties.getStorageDir(), e);
        }
    }

    public Path getRootStoragePath() {
        return rootStoragePath;
    }

    /**
     * Stores an individual file stream, streaming it to disk while computing SHA-256 and enforcing size limit.
     */
    public StoredFile storeFile(InputStream inputStream, String originalFilename) throws IOException {
        String safeExt = extractExtension(originalFilename);
        String storageKey = UUID.randomUUID().toString() + (safeExt.isEmpty() ? "" : "." + safeExt);
        Path targetPath = rootStoragePath.resolve(storageKey).normalize();

        // Enforce path containment
        if (!targetPath.startsWith(rootStoragePath)) {
            throw new BadRequestException("Invalid file destination path");
        }

        MessageDigest digest = createSha256Digest();
        long totalBytes = 0;

        try (DigestInputStream dis = new DigestInputStream(inputStream, digest);
             OutputStream os = Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = dis.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > storageProperties.getMaxFileSize()) {
                    Files.deleteIfExists(targetPath);
                    throw new BadRequestException(String.format("File '%s' exceeds maximum allowed size of %d MB",
                            originalFilename, storageProperties.getMaxFileSize() / (1024 * 1024)));
                }
                os.write(buffer, 0, read);
            }
        } catch (Exception e) {
            Files.deleteIfExists(targetPath);
            throw e;
        }

        String contentHash = HexFormat.of().formatHex(digest.digest());
        return new StoredFile(storageKey, targetPath, originalFilename, totalBytes, contentHash);
    }

    /**
     * Extracts and validates files from a ZIP archive stream.
     * Prevents ZIP traversal (Zip Slip), archive bombs, excessive file counts, and invalid extensions.
     */
    public List<StoredFile> extractZipArchive(InputStream zipStream, long archiveSizeBytes) throws IOException {
        if (archiveSizeBytes > storageProperties.getMaxArchiveSize()) {
            throw new BadRequestException(String.format("ZIP archive exceeds maximum size limit of %d MB",
                    storageProperties.getMaxArchiveSize() / (1024 * 1024)));
        }

        List<StoredFile> extractedFiles = new ArrayList<>();
        long totalDecompressedBytes = 0;
        int fileCount = 0;

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Skip directories and system artifacts
                if (entry.isDirectory() || isSystemArtifact(entryName)) {
                    zis.closeEntry();
                    continue;
                }

                // Check path traversal attempts
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                    cleanupFiles(extractedFiles);
                    throw new BadRequestException("Security alert: Path traversal detected in ZIP entry: " + entryName);
                }

                // Verify file extension
                String ext = extractExtension(entryName).toLowerCase();
                if (!ext.equals("pdf") && !ext.equals("docx") && !ext.equals("doc")) {
                    log.warn("Skipping non-resume file in ZIP archive: {}", entryName);
                    zis.closeEntry();
                    continue;
                }

                fileCount++;
                if (fileCount > storageProperties.getMaxFilesPerBatch()) {
                    cleanupFiles(extractedFiles);
                    throw new BadRequestException(String.format("ZIP archive contains more than %d files",
                            storageProperties.getMaxFilesPerBatch()));
                }

                // Extract single entry safely
                String originalFilename = Paths.get(entryName).getFileName().toString();
                String storageKey = UUID.randomUUID().toString() + "." + ext;
                Path targetPath = rootStoragePath.resolve(storageKey).normalize();

                if (!targetPath.startsWith(rootStoragePath)) {
                    cleanupFiles(extractedFiles);
                    throw new BadRequestException("Invalid ZIP entry path destination: " + entryName);
                }

                MessageDigest digest = createSha256Digest();
                long entryBytes = 0;

                try (OutputStream os = Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        digest.update(buffer, 0, read);
                        entryBytes += read;
                        totalDecompressedBytes += read;

                        if (entryBytes > storageProperties.getMaxFileSize()) {
                            Files.deleteIfExists(targetPath);
                            cleanupFiles(extractedFiles);
                            throw new BadRequestException(String.format("File '%s' exceeds single file size limit of %d MB",
                                    originalFilename, storageProperties.getMaxFileSize() / (1024 * 1024)));
                        }

                        if (totalDecompressedBytes > storageProperties.getMaxDecompressedSize()) {
                            Files.deleteIfExists(targetPath);
                            cleanupFiles(extractedFiles);
                            throw new BadRequestException("ZIP archive exceeds maximum decompressed size limit (Zip Bomb protection)");
                        }

                        os.write(buffer, 0, read);
                    }
                } catch (Exception e) {
                    Files.deleteIfExists(targetPath);
                    cleanupFiles(extractedFiles);
                    throw e;
                }

                String contentHash = HexFormat.of().formatHex(digest.digest());
                extractedFiles.add(new StoredFile(storageKey, targetPath, originalFilename, entryBytes, contentHash));
                zis.closeEntry();
            }
        }

        if (extractedFiles.isEmpty()) {
            throw new BadRequestException("ZIP archive contains no valid PDF or DOCX resume documents");
        }

        return extractedFiles;
    }

    public Path resolvePath(String storageKey) {
        Path path = rootStoragePath.resolve(storageKey).normalize();
        if (!path.startsWith(rootStoragePath) || !Files.exists(path)) {
            throw new BadRequestException("File not found in storage: " + storageKey);
        }
        return path;
    }

    public InputStream getFileInputStream(String storageKey) throws IOException {
        Path path = resolvePath(storageKey);
        return Files.newInputStream(path);
    }

    public void cleanupFiles(List<StoredFile> files) {
        for (StoredFile file : files) {
            try {
                Files.deleteIfExists(file.path());
            } catch (IOException e) {
                log.warn("Failed to delete file during cleanup: {}", file.path(), e);
            }
        }
    }

    private boolean isSystemArtifact(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.startsWith("__MACOSX/") ||
                normalized.contains("/.DS_Store") ||
                normalized.endsWith(".DS_Store") ||
                normalized.startsWith("._") ||
                normalized.contains("/._");
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) return "";
        return filename.substring(lastDot + 1).replaceAll("[^a-zA-Z0-9]", "");
    }

    private MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest algorithm not available", e);
        }
    }

    public record StoredFile(
            String storageKey,
            Path path,
            String originalFilename,
            long fileSizeBytes,
            String contentHash
    ) {}
}
