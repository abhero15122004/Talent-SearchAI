package com.talentmatch.user;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.common.exception.DuplicateResourceException;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.user.dto.CreateUserRequest;
import com.talentmatch.user.dto.UserDto;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import com.talentmatch.user.service.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Create user hashes password with BCrypt and never stores plaintext")
    void testCreateUser_HashesPassword() {
        CreateUserRequest request = new CreateUserRequest(
                "newrecruiter@acme.com",
                "PlaintextSecret123!",
                "Bob Recruiter",
                Role.RECRUITER,
                1L
        );

        Organization org = new Organization(1L, "Acme", "acme", "acme.com", true);

        when(userRepository.existsByEmail("newrecruiter@acme.com")).thenReturn(false);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(passwordEncoder.encode("PlaintextSecret123!")).thenReturn("$2a$12$hashedPasswordExample");

        User savedUser = new User("newrecruiter@acme.com", "$2a$12$hashedPasswordExample", "Bob Recruiter", Role.RECRUITER, org);
        savedUser.setId(10L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.createUser(request);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.email()).isEqualTo("newrecruiter@acme.com");

        // Verify password was hashed before saving
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User persistedUser = userCaptor.getValue();

        assertThat(persistedUser.getPasswordHash()).isEqualTo("$2a$12$hashedPasswordExample");
        assertThat(persistedUser.getPasswordHash()).doesNotContain("PlaintextSecret123!");
    }

    @Test
    @DisplayName("Create user with duplicate email throws DuplicateResourceException")
    void testCreateUser_DuplicateEmail_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest(
                "existing@acme.com",
                "Password123!",
                "Existing User",
                Role.RECRUITER,
                1L
        );

        when(userRepository.existsByEmail("existing@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing@acme.com");

        verify(userRepository, never()).save(any(User.class));
    }
}
