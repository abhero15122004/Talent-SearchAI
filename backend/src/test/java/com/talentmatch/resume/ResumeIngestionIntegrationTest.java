package com.talentmatch.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.candidate.entity.Candidate;
import com.talentmatch.candidate.repository.CandidateRepository;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.resume.entity.BatchStatus;
import com.talentmatch.resume.entity.ResumeBatch;
import com.talentmatch.resume.entity.ResumeFile;
import com.talentmatch.resume.entity.ResumeFileStatus;
import com.talentmatch.resume.repository.ResumeBatchRepository;
import com.talentmatch.resume.repository.ResumeFileRepository;
import com.talentmatch.security.JwtTokenProvider;
import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ResumeBatchRepository resumeBatchRepository;

    @Autowired
    private ResumeFileRepository resumeFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Organization orgA;
    private Organization orgB;
    private User recruiterA;
    private User recruiterB;
    private String recruiterAToken;
    private String recruiterBToken;

    @BeforeEach
    void setUp() {
        resumeFileRepository.deleteAll();
        resumeBatchRepository.deleteAll();
        candidateRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(new Organization("Acme Corp", "acme-corp", "acme.com"));
        recruiterA = userRepository.save(new User(
                "recruiterA@acme.com",
                passwordEncoder.encode("Password123!"),
                "Alice Recruiter",
                Role.RECRUITER,
                orgA
        ));
        recruiterAToken = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(recruiterA));

        orgB = organizationRepository.save(new Organization("Global LLC", "global-llc", "global.com"));
        recruiterB = userRepository.save(new User(
                "recruiterB@global.com",
                passwordEncoder.encode("Password123!"),
                "Bob Recruiter",
                Role.RECRUITER,
                orgB
        ));
        recruiterBToken = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(recruiterB));
    }

    @Test
    @DisplayName("Upload multiple files successfully and verify async processing and candidate extraction")
    void testUploadFiles_successAndCandidateCreation() throws Exception {
        byte[] pdfBytes = generateSyntheticPdf("Candidate One\ncandidate.one@example.com\n555-0101\nSkills\nJava, Spring Boot");

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "candidate_one.pdf",
                "application/pdf",
                pdfBytes
        );

        String responseContent = mockMvc.perform(multipart("/api/resumes/upload/files")
                        .file(file)
                        .param("batchName", "Engineering Intake")
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").isNumber())
                .andExpect(jsonPath("$.data.totalFiles").value(1))
                .andReturn().getResponse().getContentAsString();

        Long batchId = objectMapper.readTree(responseContent).path("data").path("batchId").asLong();

        // Wait up to 5 seconds for async executor to process the file
        long start = System.currentTimeMillis();
        ResumeBatch batch = null;
        while (System.currentTimeMillis() - start < 5000) {
            batch = resumeBatchRepository.findById(batchId).orElse(null);
            if (batch != null && batch.getProcessedFiles() >= 1) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(batch).isNotNull();
        assertThat(batch.getStatus()).isIn(BatchStatus.SUCCEEDED, BatchStatus.PARTIAL_SUCCESS);
        assertThat(batch.getSuccessfulFiles()).isEqualTo(1);

        // Verify Candidate profile was extracted in Org A
        assertThat(candidateRepository.findByOrganizationId(orgA.getId(), null).getTotalElements()).isEqualTo(1);
        Candidate candidate = candidateRepository.findByOrganizationId(orgA.getId(), null).getContent().get(0);
        assertThat(candidate.getFullName()).isEqualTo("Candidate One");
        assertThat(candidate.getEmail()).isEqualTo("candidate.one@example.com");
        assertThat(candidate.getPrimarySkills()).contains("Java", "Spring Boot");

        // Verify Candidate listing API
        mockMvc.perform(get("/api/candidates")
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Candidate One"));
    }

    @Test
    @DisplayName("Duplicate resume submission marks duplicate file SKIPPED_DUPLICATE without duplicating candidate")
    void testUploadDuplicateResume_marksSkippedDuplicate() throws Exception {
        byte[] pdfBytes = generateSyntheticPdf("Duplicate Candidate\ndup@example.com\n555-9999\nSkills\nPython, Docker");

        MockMultipartFile file1 = new MockMultipartFile("files", "resume1.pdf", "application/pdf", pdfBytes);
        mockMvc.perform(multipart("/api/resumes/upload/files")
                        .file(file1)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isAccepted());

        // Wait for first file to complete
        long start1 = System.currentTimeMillis();
        while (System.currentTimeMillis() - start1 < 5000) {
            if (candidateRepository.findByOrganizationId(orgA.getId(), null).getTotalElements() >= 1) {
                break;
            }
            Thread.sleep(100);
        }
        assertThat(candidateRepository.findByOrganizationId(orgA.getId(), null).getTotalElements()).isEqualTo(1);

        // Upload exact same file again in another batch
        MockMultipartFile file2 = new MockMultipartFile("files", "resume2_copy.pdf", "application/pdf", pdfBytes);
        String secondResponse = mockMvc.perform(multipart("/api/resumes/upload/files")
                        .file(file2)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        Long secondBatchId = objectMapper.readTree(secondResponse).path("data").path("batchId").asLong();

        // Wait for second batch to process
        long start2 = System.currentTimeMillis();
        ResumeBatch secondBatch = null;
        while (System.currentTimeMillis() - start2 < 5000) {
            secondBatch = resumeBatchRepository.findById(secondBatchId).orElse(null);
            if (secondBatch != null && secondBatch.getProcessedFiles() >= 1) {
                break;
            }
            Thread.sleep(100);
        }
        assertThat(secondBatch).isNotNull();
        assertThat(secondBatch.getDuplicateFiles()).isEqualTo(1);

        // Candidate count must remain 1
        assertThat(candidateRepository.findByOrganizationId(orgA.getId(), null).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Upload ZIP archive and extract resume files asynchronously")
    void testUploadZipArchive_extractsAndProcesses() throws Exception {
        byte[] pdfBytes = generateSyntheticPdf("Zip Candidate\nzip.user@example.com\n555-4321\nSkills\nReact, TypeScript");
        byte[] zipBytes = createZipWithSingleFile("zip_candidate.pdf", pdfBytes);

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "candidates_archive.zip",
                "application/zip",
                zipBytes
        );

        mockMvc.perform(multipart("/api/resumes/upload/zip")
                        .file(zipFile)
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.batchId").isNumber())
                .andExpect(jsonPath("$.data.totalFiles").value(1));
    }

    @Test
    @DisplayName("Tenant isolation: Recruiter B cannot view or access batches or candidates from Org A")
    void testTenantIsolation_batchAndCandidateAccessDenied() throws Exception {
        // Create candidate in Org A directly
        Candidate candidate = candidateRepository.save(new Candidate(
                orgA, "Alice Private", "private@acme.com", "555-0000",
                "Private Resume Text", "Summary", "Java", "BS", "Exp", "Proj", "private.pdf"
        ));

        // Create batch in Org A
        ResumeBatch batch = resumeBatchRepository.save(new ResumeBatch(orgA, recruiterA, "Private Batch", 1));

        // Recruiter B tries to get Org A candidate
        mockMvc.perform(get("/api/candidates/" + candidate.getId())
                        .header("Authorization", "Bearer " + recruiterBToken))
                .andExpect(status().isNotFound());

        // Recruiter B tries to get Org A batch
        mockMvc.perform(get("/api/resumes/batches/" + batch.getId())
                        .header("Authorization", "Bearer " + recruiterBToken))
                .andExpect(status().isNotFound());

        // Recruiter B lists candidates: should see 0
        mockMvc.perform(get("/api/candidates")
                        .header("Authorization", "Bearer " + recruiterBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
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

    private byte[] createZipWithSingleFile(String filename, byte[] content) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(filename);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
            zos.finish();
            return baos.toByteArray();
        }
    }
}
