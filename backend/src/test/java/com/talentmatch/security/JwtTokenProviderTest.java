package com.talentmatch.security;

import com.talentmatch.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Generate token and validate username extraction")
    void testGenerateAndValidateToken() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "recruiter@acme.com",
                "hashedPass",
                "Alice Recruiter",
                Role.RECRUITER,
                10L,
                "Acme Corp",
                true
        );

        String token = tokenProvider.generateTokenForUser(principal);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromJwt(token)).isEqualTo("recruiter@acme.com");
    }

    @Test
    @DisplayName("Validate token returns false for invalid or tampered token")
    void testValidateToken_InvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

        assertThat(tokenProvider.validateToken(invalidToken)).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }
}
