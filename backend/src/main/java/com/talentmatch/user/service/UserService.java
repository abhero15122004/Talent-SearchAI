package com.talentmatch.user.service;

import com.talentmatch.common.dto.PagedResponse;
import com.talentmatch.user.dto.CreateUserRequest;
import com.talentmatch.user.dto.UserDto;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserDto createUser(CreateUserRequest request);

    UserDto getUserById(Long id);

    UserDto getUserByEmail(String email);

    PagedResponse<UserDto> getUsersForOrganization(Long organizationId, Pageable pageable);

    PagedResponse<UserDto> getAllUsers(Pageable pageable);

    void deactivateUser(Long id);
}
