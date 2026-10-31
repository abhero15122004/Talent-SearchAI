package com.talentmatch.resume.service;

import com.talentmatch.common.exception.BadRequestException;
import org.apache.tika.Tika;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TikaParserService {

    private static final Logger log = LoggerFactory.getLogger(TikaParserService.class);

    private final Tika tika;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{2,4}\\)?[-.\\s]?)?\\d{3,4}[-.\\s]?\\d{3,4}"
    );

    private static final List<String> COMMON_SKILLS = List.of(
            "Java", "Spring", "Spring Boot", "Hibernate", "JPA", "Microservices",
            "React", "TypeScript", "JavaScript", "Next.js", "Angular", "Vue", "Node.js",
            "Python", "FastAPI", "Django", "Flask", "Pandas", "NumPy", "PyTorch", "TensorFlow",
            "SQL", "MySQL", "PostgreSQL", "Oracle", "MongoDB", "Redis", "Elasticsearch",
            "Docker", "Kubernetes", "AWS", "Azure", "GCP", "CI/CD", "Git", "GitHub Actions",
            "REST", "RESTful", "GraphQL", "gRPC", "Kafka", "RabbitMQ", "Linux", "Bash",
            "C++", "C#", ".NET", "Go", "Golang", "Rust", "HTML", "CSS", "Tailwind", "Bootstrap"
    );

    public TikaParserService() {
        this.tika = new Tika();
        // Prevent parser from truncating large resume text
        this.tika.setMaxStringLength(5 * 1024 * 1024);
    }

    /**
     * Validates file MIME magic bytes and extracts full textual content.
     */
    public ParsedDocument parseDocument(InputStream inputStream, String originalFilename) {
        try {
            // Buffer input stream so Tika can inspect headers and reset if needed
            BufferedInputStream bis = new BufferedInputStream(inputStream);

            String detectedMime = tika.detect(bis, originalFilename);
            if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
                throw new BadRequestException(String.format("Unsupported file format '%s' for '%s'. Only PDF and DOCX documents are accepted.",
                        detectedMime, originalFilename));
            }

            String rawText = tika.parseToString(bis);
            if (rawText == null || rawText.trim().isEmpty()) {
                throw new BadRequestException("Document contains no readable text or is an empty/scanned image file");
            }
            // Normalize non-breaking spaces and mojibake artifacts from PDF/DOCX charsets
            rawText = cleanText(rawText);

            ParsedCandidateProfile profile = extractCandidateProfile(rawText, originalFilename);
            return new ParsedDocument(detectedMime, rawText.trim(), profile);

        } catch (EncryptedDocumentException e) {
            log.warn("Encrypted document rejected: {}", originalFilename);
            throw new BadRequestException("Document is password protected or encrypted and cannot be parsed");
        } catch (TikaException e) {
            log.error("Tika extraction error for {}: {}", originalFilename, e.getMessage());
            throw new BadRequestException("Malformed or corrupt document structure: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error while reading {}: {}", originalFilename, e.getMessage());
            throw new BadRequestException("Failed to read document stream: " + e.getMessage());
        }
    }

    /**
     * Extracts structured candidate information via heuristics and pattern matching.
     * Guaranteed never to hallucinate or fabricate information.
     */
    public ParsedCandidateProfile extractCandidateProfile(String rawText, String sourceFilename) {
        String fullName = extractName(rawText, sourceFilename);
        String email = extractEmail(rawText);
        String phone = extractPhone(rawText);
        String skills = extractSkills(rawText);

        Map<String, String> sections = extractSections(rawText);
        String summary = sections.get("summary");
        String education = sections.get("education");
        String experience = sections.get("experience");
        String projects = sections.get("projects");
        String honors = sections.get("honors");
        String activities = sections.get("activities");
        String links = sections.get("links");

        StringBuilder projectBuilder = new StringBuilder();
        if (projects != null && !projects.isBlank()) {
            projectBuilder.append(projects);
        }
        if (honors != null && !honors.isBlank()) {
            if (projectBuilder.length() > 0) projectBuilder.append("\n\n");
            projectBuilder.append("Honours and Distinctions:\n").append(honors);
        }
        if (activities != null && !activities.isBlank()) {
            if (projectBuilder.length() > 0) projectBuilder.append("\n\n");
            projectBuilder.append("Extra-Curricular Activities:\n").append(activities);
        }
        if (links != null && !links.isBlank()) {
            if (projectBuilder.length() > 0) projectBuilder.append("\n\n");
            projectBuilder.append("Websites & Links:\n").append(links);
        }
        String finalProjects = projectBuilder.length() > 0 ? projectBuilder.toString() : null;

        return new ParsedCandidateProfile(
                fullName,
                email,
                phone,
                summary,
                skills,
                education,
                experience,
                finalProjects
        );
    }

    private String extractName(String rawText, String sourceFilename) {
        String[] lines = rawText.split("\\r?\\n");
        for (int i = 0; i < Math.min(lines.length, 8); i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() < 2 || line.length() > 60) {
                continue;
            }
            String lower = line.toLowerCase();
            if (lower.contains("resume") || lower.contains("curriculum") || lower.contains("cv") ||
                lower.contains("page") || lower.contains("@") || lower.contains("http") ||
                lower.contains("phone") || lower.contains("email") || lower.contains("summary") ||
                lower.contains("developer") || lower.contains("engineer")) {
                continue;
            }
            // Check if line looks like a person's name (letters, spaces, dots, hyphens)
            if (line.matches("^[a-zA-Z\\s.\\-']+$") && line.split("\\s+").length <= 4) {
                return line;
            }
        }

        // Fallback: deduce from filename if it looks like "FirstName_LastName_Resume.pdf"
        if (sourceFilename != null && !sourceFilename.isEmpty()) {
            String base = sourceFilename.replaceFirst("[.][^.]+$", "");
            base = base.replaceAll("(?i)(resume|cv|latest|updated|final|[0-9_\\-\\(\\)])", " ").trim();
            if (!base.isEmpty() && base.length() >= 3 && base.matches("^[a-zA-Z\\s]+$")) {
                return base;
            }
        }
        return null;
    }

    private String extractEmail(String rawText) {
        Matcher matcher = EMAIL_PATTERN.matcher(rawText);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    private String extractPhone(String rawText) {
        Matcher matcher = PHONE_PATTERN.matcher(rawText);
        while (matcher.find()) {
            String match = matcher.group().trim();
            String digitsOnly = match.replaceAll("\\D", "");
            if (digitsOnly.length() >= 10 && digitsOnly.length() <= 15) {
                return match;
            }
        }
        return null;
    }

    private String extractSkills(String rawText) {
        Set<String> detectedSkills = new LinkedHashSet<>();
        String lowerText = rawText.toLowerCase();

        // 1. Explicit skill section search
        Map<String, String> sections = extractSections(rawText);
        String skillSection = sections.get("skills");
        if (skillSection != null) {
            String[] tokens = skillSection.split("[,;\\n•|/]");
            for (String token : tokens) {
                String clean = token.trim();
                if (!clean.isEmpty() && clean.length() < 40 && !clean.toLowerCase().contains("skill")) {
                    for (String common : COMMON_SKILLS) {
                        if (clean.equalsIgnoreCase(common)) {
                            detectedSkills.add(common);
                        }
                    }
                }
            }
        }

        // 2. Scan text against common skill dictionary
        for (String skill : COMMON_SKILLS) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(skill.toLowerCase()) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(lowerText).find()) {
                detectedSkills.add(skill);
            }
        }

        if (detectedSkills.isEmpty()) {
            return null;
        }
        return String.join(", ", detectedSkills);
    }

    public static String cleanText(String text) {
        if (text == null) return null;
        return text.replace("\u00A0", " ")
                   .replace("Â", "")
                   .replace("\"˜", "\"")
                   .replace("˜", "")
                   .replace("â€™", "'")
                   .replace("â€œ", "\"")
                   .replace("â€\u009d", "\"")
                   .replace("â€", "\"")
                   .replace("â€\"", "—")
                   .replaceAll("\\s+,", ",")
                   .replaceAll("[ \\t]+", " ")
                   .trim();
    }

    private Map<String, String> extractSections(String rawText) {
        Map<String, String> sections = new HashMap<>();
        // Inject newlines before known section headers if they are glued on the same line
        String preprocessed = rawText
                .replaceAll("(?i)(?<=[.?!;\\s])(Education\\s*&\\s*Qualifications|Work\\s*Experience|Honours?\\s*(?:and|&)?\\s*Distinctions?|Extra-?Curricular\\s*Activities|Website(?:s)?\\s*(?:and|&)?\\s*Links|Key\\s*Projects|Personal\\s*Projects)(?=[\\s:\\n])", "\n$1\n");

        String[] lines = preprocessed.split("\\r?\\n");

        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            String identified = identifySectionHeader(trimmed);

            if (identified != null) {
                if (currentSection != null && currentContent.length() > 0) {
                    sections.put(currentSection, currentContent.toString().trim());
                }
                currentSection = identified;
                currentContent = new StringBuilder();
            } else if (currentSection != null) {
                if (!trimmed.isEmpty()) {
                    currentContent.append(trimmed).append("\n");
                }
            }
        }

        if (currentSection != null && currentContent.length() > 0) {
            sections.put(currentSection, currentContent.toString().trim());
        }

        return sections;
    }

    private String identifySectionHeader(String line) {
        if (line.length() > 60 || line.isEmpty()) {
            return null;
        }
        String normalized = line.toLowerCase().replaceAll("[^a-z]", " ").replaceAll("\\s+", " ").trim();

        if (normalized.equals("summary") || normalized.equals("professional summary") || normalized.equals("profile") ||
            normalized.equals("about me") || normalized.equals("executive summary") || normalized.equals("career objective")) {
            return "summary";
        }
        if (normalized.equals("skills") || normalized.equals("technical skills") || normalized.equals("core competencies") ||
            normalized.equals("technologies") || normalized.equals("skills and tools") || normalized.equals("tech stack") ||
            normalized.equals("key skills") || normalized.equals("tools and technologies")) {
            return "skills";
        }
        if (normalized.equals("experience") || normalized.equals("work experience") || normalized.equals("professional experience") ||
            normalized.equals("employment history") || normalized.equals("work history") || normalized.equals("relevant experience")) {
            return "experience";
        }
        if (normalized.equals("education") || normalized.equals("academic background") || normalized.equals("degrees") ||
            normalized.equals("academics") || normalized.equals("education qualifications") || normalized.equals("education and qualifications") ||
            normalized.equals("qualifications")) {
            return "education";
        }
        if (normalized.equals("projects") || normalized.equals("personal projects") || normalized.equals("technical projects") ||
            normalized.equals("key projects") || normalized.equals("portfolio") || normalized.equals("academic projects")) {
            return "projects";
        }
        if (normalized.equals("honours and distinction") || normalized.equals("honors and distinctions") ||
            normalized.equals("honors and awards") || normalized.equals("awards") || normalized.equals("honors") ||
            normalized.equals("achievements") || normalized.equals("awards and achievements") ||
            normalized.equals("honours and distinctions")) {
            return "honors";
        }
        if (normalized.equals("extra curricular activities") || normalized.equals("extracurricular activities") ||
            normalized.equals("activities") || normalized.equals("leadership") || normalized.equals("volunteer experience")) {
            return "activities";
        }
        if (normalized.equals("website and links") || normalized.equals("links") || normalized.equals("websites") ||
            normalized.equals("online presence") || normalized.equals("social links")) {
            return "links";
        }
        return null;
    }

    public record ParsedDocument(String contentType, String rawText, ParsedCandidateProfile profile) {}

    public record ParsedCandidateProfile(
            String fullName,
            String email,
            String phone,
            String summary,
            String primarySkills,
            String educationHistory,
            String experienceHistory,
            String projectHistory
    ) {}
}
