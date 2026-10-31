package com.talentmatch.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmatch.auth.dto.LoginRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = organizationRepository.save(new Organization("Test Corp", "test-corp", "test.com"));
        testUser = new User(
                "recruiter@test.com",
                passwordEncoder.encode("SecurePassword123!"),
                "Recruiter One",
                Role.RECRUITER,
                testOrg
        );
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials returns JWT token")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest("recruiter@test.com", "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("recruiter@test.com"))
                .andExpect(jsonPath("$.data.user.role").value("RECRUITER"))
                .andExpect(jsonPath("$.data.user.organizationName").value("Test Corp"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password returns 401 Unauthorized")
    void testLogin_InvalidPassword_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("recruiter@test.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email address or password."));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid Bearer token returns current user identity")
    void testGetMe_Success() throws Exception {
        String token = jwtTokenProvider.generateTokenForUser(UserPrincipal.create(testUser));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("recruiter@test.com"))
                .andExpect(jsonPath("$.data.fullName").value("Recruiter One"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without token returns 401 Unauthorized")
    void testGetMe_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
