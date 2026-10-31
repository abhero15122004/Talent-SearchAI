package com.talentmatch.user.controller;

import com.talentmatch.common.dto.ApiResponse;
import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.user.dto.CreateUserRequest;
import com.talentmatch.user.dto.UserDto;
import com.talentmatch.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management and recruiter team endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users for the current organization or platform")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> listUsers(
            @RequestParam(required = false) Long organizationId,
            @PageableDefault(size = 20) Pageable pageable) {

        Long effectiveOrgId = organizationId != null ? organizationId : SecurityUtils.getCurrentOrganizationId();

        PagedResponse<UserDto> response;
        if (effectiveOrgId != null) {
            response = userService.getUsersForOrganization(effectiveOrgId, pageable);
        } else if (SecurityUtils.isCurrentUserAdmin()) {
            response = userService.getAllUsers(pageable);
        } else {
            response = new PagedResponse<>();
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (Tenant scoped)")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    @Operation(summary = "Create user account within organization scope")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        // If recruiter doesn't supply orgId, default to their own organization
        Long orgId = request.organizationId() != null
                ? request.organizationId()
                : SecurityUtils.getCurrentOrganizationId();

        CreateUserRequest scopedRequest = new CreateUserRequest(
                request.email(),
                request.password(),
                request.fullName(),
                request.role(),
                orgId
        );

        UserDto created = userService.createUser(scopedRequest);
        return new ResponseEntity<>(ApiResponse.success(created, "User created successfully"), HttpStatus.CREATED);
    }
}
