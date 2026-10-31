package com.talentmatch.resume;

import com.talentmatch.common.exception.BadRequestException;
import com.talentmatch.config.StorageProperties;
import com.talentmatch.resume.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTest {

    private StorageService storageService;
    private StorageProperties storageProperties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setStorageDir(tempDir.toString());
        storageProperties.setMaxFileSize(1024 * 1024); // 1MB
        storageProperties.setMaxArchiveSize(5 * 1024 * 1024); // 5MB
        storageProperties.setMaxDecompressedSize(10 * 1024 * 1024); // 10MB
        storageProperties.setMaxFilesPerBatch(5);

        storageService = new StorageService(storageProperties);
        storageService.init();
    }

    @Test
    void testStoreFile_computesSha256AndStoresSuccessfully() throws IOException {
        String content = "Candidate Resume for Alice Smith";
        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        StorageService.StoredFile stored = storageService.storeFile(is, "Alice_Resume.pdf");

        assertThat(stored).isNotNull();
        assertThat(stored.originalFilename()).isEqualTo("Alice_Resume.pdf");
        assertThat(stored.fileSizeBytes()).isEqualTo(content.getBytes(StandardCharsets.UTF_8).length);
        assertThat(stored.contentHash()).isNotBlank();
        assertThat(stored.contentHash()).hasSize(64); // SHA-256 hex length
        assertThat(stored.storageKey()).endsWith(".pdf");
    }

    @Test
    void testStoreFile_exceedingMaxFileSize_throwsBadRequestException() {
        storageProperties.setMaxFileSize(50); // 50 bytes limit

        byte[] largeBytes = new byte[200];
        ByteArrayInputStream is = new ByteArrayInputStream(largeBytes);

        assertThatThrownBy(() -> storageService.storeFile(is, "large.pdf"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    void testExtractZipArchive_validZip_extractsSuccessfully() throws IOException {
        byte[] zipBytes = createSyntheticZip(List.of(
                new SyntheticEntry("resume1.pdf", "Content of resume 1"),
                new SyntheticEntry("resume2.docx", "Content of resume 2")
        ));

        List<StorageService.StoredFile> files = storageService.extractZipArchive(
                new ByteArrayInputStream(zipBytes), zipBytes.length);

        assertThat(files).hasSize(2);
        assertThat(files.get(0).originalFilename()).isEqualTo("resume1.pdf");
        assertThat(files.get(1).originalFilename()).isEqualTo("resume2.docx");
        assertThat(files.get(0).contentHash()).isNotEqualTo(files.get(1).contentHash());
    }

    @Test
    void testExtractZipArchive_pathTraversal_rejectsSecurityThreat() {
        byte[] zipBytes = createSyntheticZip(List.of(
                new SyntheticEntry("../../etc/passwd.pdf", "malicious traversal content")
        ));

        assertThatThrownBy(() -> storageService.extractZipArchive(
                new ByteArrayInputStream(zipBytes), zipBytes.length))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Path traversal detected");
    }

    @Test
    void testExtractZipArchive_exceedingMaxFiles_throwsBadRequestException() {
        storageProperties.setMaxFilesPerBatch(2);

        byte[] zipBytes = createSyntheticZip(List.of(
                new SyntheticEntry("doc1.pdf", "1"),
                new SyntheticEntry("doc2.pdf", "2"),
                new SyntheticEntry("doc3.pdf", "3")
        ));

        assertThatThrownBy(() -> storageService.extractZipArchive(
                new ByteArrayInputStream(zipBytes), zipBytes.length))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("contains more than 2 files");
    }

    @Test
    void testExtractZipArchive_zipBombDecompressedLimit_throwsBadRequestException() {
        storageProperties.setMaxDecompressedSize(100); // 100 bytes limit

        byte[] largeContent = new byte[500];
        byte[] zipBytes = createSyntheticZip(List.of(
                new SyntheticEntry("large.pdf", new String(largeContent, StandardCharsets.UTF_8))
        ));

        assertThatThrownBy(() -> storageService.extractZipArchive(
                new ByteArrayInputStream(zipBytes), zipBytes.length))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Zip Bomb protection");
    }

    private record SyntheticEntry(String name, String content) {}

    private byte[] createSyntheticZip(List<SyntheticEntry> entries) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (SyntheticEntry entry : entries) {
                ZipEntry ze = new ZipEntry(entry.name);
                zos.putNextEntry(ze);
                zos.write(entry.content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
