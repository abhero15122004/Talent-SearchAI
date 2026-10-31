package com.talentmatch.resume;

import com.talentmatch.common.exception.BadRequestException;
import com.talentmatch.resume.service.TikaParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TikaParserServiceTest {

    private TikaParserService tikaParserService;

    @BeforeEach
    void setUp() {
        tikaParserService = new TikaParserService();
    }

    @Test
    void testExtractCandidateProfile_completeResume_extractsAllSectionsAccurately() {
        String resumeText = """
                Jane Doe
                Senior Software Engineer
                Email: jane.doe@talentmatch.ai | Phone: +1 415-555-2671
                
                Summary
                Versatile full stack developer with 7+ years of experience designing scalable microservices and web apps.
                
                Skills
                Java, Spring Boot, React, TypeScript, Docker, Kubernetes, MySQL, Redis, AWS
                
                Experience
                Senior Software Engineer - Tech Solutions Inc. (2021 - Present)
                - Architected high-throughput ingestion pipeline handling 5M daily requests.
                
                Education
                Bachelor of Science in Computer Science - University of California, Berkeley (2017)
                
                Projects
                Cloud Distributed Cache - Open-source in-memory cache written in Go.
                """;

        TikaParserService.ParsedCandidateProfile profile =
                tikaParserService.extractCandidateProfile(resumeText, "Jane_Doe_Resume.pdf");

        assertThat(profile).isNotNull();
        assertThat(profile.fullName()).isEqualTo("Jane Doe");
        assertThat(profile.email()).isEqualTo("jane.doe@talentmatch.ai");
        assertThat(profile.phone()).contains("415-555-2671");
        assertThat(profile.summary()).contains("Versatile full stack developer");
        assertThat(profile.primarySkills()).contains("Java", "Spring Boot", "React", "TypeScript", "Docker");
        assertThat(profile.educationHistory()).contains("University of California, Berkeley");
        assertThat(profile.experienceHistory()).contains("Tech Solutions Inc");
        assertThat(profile.projectHistory()).contains("Cloud Distributed Cache");
    }

    @Test
    void testExtractCandidateProfile_minimalResume_neverHallucinatesMissingSections() {
        String minimalText = """
                John Smith
                john.smith@example.org
                555-234-5678
                """;

        TikaParserService.ParsedCandidateProfile profile =
                tikaParserService.extractCandidateProfile(minimalText, "John_Smith.pdf");

        assertThat(profile.fullName()).isEqualTo("John Smith");
        assertThat(profile.email()).isEqualTo("john.smith@example.org");
        assertThat(profile.phone()).contains("555-234-5678");

        // Verify zero-fabrication / unhallucinated:
        assertThat(profile.summary()).isNull();
        assertThat(profile.educationHistory()).isNull();
        assertThat(profile.experienceHistory()).isNull();
        assertThat(profile.projectHistory()).isNull();
    }

    @Test
    void testParseDocument_validPdf_extractsSuccessfully() throws IOException {
        byte[] pdfBytes = generateSyntheticPdf("Alice Johnson\nalice@example.com\n555-987-6543\nSkills\nJava, Python, SQL");

        TikaParserService.ParsedDocument doc = tikaParserService.parseDocument(
                new ByteArrayInputStream(pdfBytes), "Alice_Resume.pdf");

        assertThat(doc.contentType()).isEqualTo("application/pdf");
        assertThat(doc.rawText()).contains("Alice Johnson");
        assertThat(doc.profile().fullName()).isEqualTo("Alice Johnson");
        assertThat(doc.profile().email()).isEqualTo("alice@example.com");
    }

    @Test
    void testParseDocument_unsupportedMimeType_throwsBadRequestException() {
        byte[] plainBytes = "This is a plain text file, not a real PDF".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> tikaParserService.parseDocument(
                new ByteArrayInputStream(plainBytes), "fake.txt"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported file format");
    }

    private byte[] generateSyntheticPdf(String text) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.beginText();
                cs.setLeading(14.5f);
                cs.newLineAtOffset(50, 700);

                for (String line : text.split("\n")) {
                    cs.showText(line.trim());
                    cs.newLine();
                }
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
