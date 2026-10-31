package com.talentmatch.user.repository;

import com.talentmatch.user.entity.Role;
import com.talentmatch.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organization WHERE u.email = :email")
    Optional<User> findByEmailWithOrganization(@Param("email") String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrganizationId(Long organizationId);

    Page<User> findAllByOrganizationId(Long organizationId, Pageable pageable);

    long countByRole(Role role);
}
