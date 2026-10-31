package com.talentmatch.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.job.repository.JobRepository;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.security.JwtTokenProvider;
import com.talentmatch.security.UserPrincipal;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Organization orgA;
    private Organization orgB;
    private String recruiterAToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organization A & Recruiter A
        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp", "alpha.com"));
        User recruiterA = new User("recruiterA@alpha.com", passwordEncoder.encode("Pass123!"), "Alice Recruiter", Role.RECRUITER, orgA);
        userRepository.save(recruiterA);
        recruiterAToken = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(recruiterA));

        // 2. Create Organization B
        orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc", "beta.com"));

        // 3. Create Global Admin
        User admin = new User("admin@talentmatch.ai", passwordEncoder.encode("AdminPass123!"), "Platform Admin", Role.ADMIN, orgA);
        userRepository.save(admin);
        adminToken = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(admin));
    }

    @Test
    @DisplayName("Recruiter can access their own organization data")
    void testRecruiterAccessOwnOrganization_Success() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + orgA.getId())
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("alpha-corp"));
    }

    @Test
    @DisplayName("Recruiter is blocked with 403 Forbidden when attempting to access another organization")
    void testRecruiterAccessOtherOrganization_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + orgB.getId())
                        .header("Authorization", "Bearer " + recruiterAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You do not have permission to access resources outside your assigned organization."));
    }

    @Test
    @DisplayName("Admin can access any organization across tenants")
    void testAdminAccessAnyOrganization_Success() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + orgB.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("beta-llc"));
    }
}
