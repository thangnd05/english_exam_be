package com.example.english_exam.repositories;

import com.example.english_exam.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByRoleName(String roleName);

    Optional<Role> findByRoleId(Long roleId);

}