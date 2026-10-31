package com.talentmatch.config;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import com.talentmatch.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Development-only initialization for bootstrapping seed tenants and administrator credentials.
 * Guarded by app.security.init-dev-data=true; never executes in production environments.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${app.security.init-dev-data:false}")
    private boolean initDevData;

    @Value("${ADMIN_INITIAL_PASSWORD:Admin123!}")
    private String configuredAdminPassword;

    @Value("${RECRUITER_INITIAL_PASSWORD:Recruiter123!}")
    private String configuredRecruiterPassword;

    public DataInitializer(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!initDevData) {
            log.info("Development data initialization is disabled (app.security.init-dev-data=false).");
            return;
        }

        log.info("Starting development data initialization...");

        // 1. Initialize Default Tenant Organization
        Organization organization = organizationRepository.findBySlug("acme-recruitment")
                .orElseGet(() -> {
                    Organization org = new Organization(
                            "Acme Recruitment Solutions",
                            "acme-recruitment",
                            "acmerecruitment.com"
                    );
                    return organizationRepository.save(org);
                });

        // 2. Initialize or Synchronize Administrator Account
        String adminPassword = (configuredAdminPassword != null && !configuredAdminPassword.isBlank())
                ? configuredAdminPassword
                : "Admin123!";

        User admin = userRepository.findByEmail("admin@talentmatch.ai").orElse(null);
        if (admin == null) {
            admin = new User(
                    "admin@talentmatch.ai",
                    passwordEncoder.encode(adminPassword),
                    "Platform Administrator",
                    Role.ADMIN,
                    organization
            );
            userRepository.save(admin);

            auditLogService.recordEvent(
                    organization.getId(),
                    admin.getId(),
                    admin.getEmail(),
                    "INITIALIZE_ADMIN",
                    "User",
                    String.valueOf(admin.getId()),
                    "Development administrator initialized",
                    "127.0.0.1"
            );
        } else {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
        }

        log.info("=================================================================");
        log.info("DEV ADMIN READY");
        log.info("Email:    admin@talentmatch.ai");
        log.info("Password: {}", adminPassword);
        log.info("=================================================================");

        // 3. Initialize or Synchronize Demo Recruiter Account
        String recruiterPassword = (configuredRecruiterPassword != null && !configuredRecruiterPassword.isBlank())
                ? configuredRecruiterPassword
                : "Recruiter123!";

        User recruiter = userRepository.findByEmail("recruiter@talentmatch.ai").orElse(null);
        if (recruiter == null) {
            recruiter = new User(
                    "recruiter@talentmatch.ai",
                    passwordEncoder.encode(recruiterPassword),
                    "Jane Recruiter",
                    Role.RECRUITER,
                    organization
            );
            userRepository.save(recruiter);

            auditLogService.recordEvent(
                    organization.getId(),
                    recruiter.getId(),
                    recruiter.getEmail(),
                    "INITIALIZE_RECRUITER",
                    "User",
                    String.valueOf(recruiter.getId()),
                    "Development recruiter initialized",
                    "127.0.0.1"
            );
        } else {
            recruiter.setPasswordHash(passwordEncoder.encode(recruiterPassword));
            userRepository.save(recruiter);
        }

        log.info("=================================================================");
        log.info("DEV RECRUITER READY");
        log.info("Organization: {} (Slug: {})", organization.getName(), organization.getSlug());
        log.info("Email:        recruiter@talentmatch.ai");
        log.info("Password:     {}", recruiterPassword);
        log.info("=================================================================");
    }
}
