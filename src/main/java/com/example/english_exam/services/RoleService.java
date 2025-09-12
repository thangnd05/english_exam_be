package com.example.english_exam.services;

import com.example.english_exam.models.Roles;
import com.example.english_exam.repositories.RoleRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Roles> getAllRoles() {
        return roleRepository.findAll();
    }

    public Roles getRoleById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    public Roles createRole(Roles role) {
        return roleRepository.save(role);
    }

    public Roles updateRole(Long id, Roles roleDetails) {
        return roleRepository.findById(id)
                .map(role -> {
                    role.setRoleName(roleDetails.getRoleName());
                    role.setDescription(roleDetails.getDescription());
                    return roleRepository.save(role);
                })
                .orElse(null);
    }

    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}
