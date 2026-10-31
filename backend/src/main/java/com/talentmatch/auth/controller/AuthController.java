package com.talentmatch.auth.controller;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.auth.dto.AuthUserDto;
import com.talentmatch.auth.dto.LoginRequest;
import com.talentmatch.auth.dto.LoginResponse;
import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.security.JwtTokenProvider;
import com.talentmatch.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User login, session inspection, and token verification")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials and issue JWT bearer token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        log.info("Processing login request for: {}", loginRequest.email());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email().toLowerCase().trim(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        auditLogService.recordEvent(
                userPrincipal.getOrganizationId(),
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                "LOGIN_SUCCESS",
                "User",
                String.valueOf(userPrincipal.getId()),
                "User successfully logged in",
                request.getRemoteAddr()
        );

        AuthUserDto authUser = AuthUserDto.from(userPrincipal);
        LoginResponse response = LoginResponse.of(jwt, tokenProvider.getExpirationMs(), authUser);

        return ResponseEntity.ok(ApiResponse.success(response, "Authentication successful"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out current user and record security audit log")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        SecurityUtils.getCurrentUserPrincipal().ifPresent(principal -> {
            auditLogService.recordEvent(
                    principal.getOrganizationId(),
                    principal.getId(),
                    principal.getUsername(),
                    "LOGOUT",
                    "User",
                    String.valueOf(principal.getId()),
                    "User logged out",
                    request.getRemoteAddr()
            );
        });

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Successfully logged out. Please discard token on client."));
    }

    @GetMapping("/me")
    @Operation(summary = "Inspect current authenticated user identity and tenant scope")
    public ResponseEntity<ApiResponse<AuthUserDto>> getCurrentUser() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new com.talentmatch.common.exception.ApiException(
                        "Authentication required to inspect session profile.",
                        org.springframework.http.HttpStatus.UNAUTHORIZED));

        return ResponseEntity.ok(ApiResponse.success(AuthUserDto.from(principal)));
    }
}
