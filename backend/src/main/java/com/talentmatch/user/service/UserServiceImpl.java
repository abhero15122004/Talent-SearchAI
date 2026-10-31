package com.talentmatch.user.service;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.exception.DuplicateResourceException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.user.dto.CreateUserRequest;
import com.talentmatch.user.dto.UserDto;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserServiceImpl(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("A user with email '" + normalizedEmail + "' already exists.");
        }

        Organization org = null;
        if (request.organizationId() != null) {
            SecurityUtils.validateOrganizationAccess(request.organizationId());
            org = organizationRepository.findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", request.organizationId()));
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.role(),
                org
        );

        User saved = userRepository.save(user);

        auditLogService.recordEvent(
                org != null ? org.getId() : null,
                saved.getId(),
                saved.getEmail(),
                "CREATE_USER",
                "User",
                String.valueOf(saved.getId()),
                "Created user account with role " + saved.getRole(),
                "internal"
        );

        return UserDto.from(saved);
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getOrganization() != null) {
            SecurityUtils.validateOrganizationAccess(user.getOrganization().getId());
        }

        return UserDto.from(user);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmailWithOrganization(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getOrganization() != null) {
            SecurityUtils.validateOrganizationAccess(user.getOrganization().getId());
        }

        return UserDto.from(user);
    }

    @Override
    public PagedResponse<UserDto> getUsersForOrganization(Long organizationId, Pageable pageable) {
        SecurityUtils.validateOrganizationAccess(organizationId);

        Page<User> page = userRepository.findAllByOrganizationId(organizationId, pageable);
        return PagedResponse.from(page.map(UserDto::from));
    }

    @Override
    public PagedResponse<UserDto> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return PagedResponse.from(page.map(UserDto::from));
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getOrganization() != null) {
            SecurityUtils.validateOrganizationAccess(user.getOrganization().getId());
        }

        user.setActive(false);
        userRepository.save(user);

        auditLogService.recordEvent(
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getId(),
                user.getEmail(),
                "DEACTIVATE_USER",
                "User",
                String.valueOf(user.getId()),
                "Deactivated user account",
                "internal"
        );
    }
}
