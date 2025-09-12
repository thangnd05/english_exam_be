package com.example.english_exam.services;

import com.example.english_exam.models.Role;
import com.example.english_exam.repositories.RoleRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public Role save(Role role) {
        return roleRepository.save(role);
    }



    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}
