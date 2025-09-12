package com.example.english_exam.controllers;

import com.example.english_exam.models.Role;
import com.example.english_exam.services.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // Lấy tất cả roles
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    // Lấy role theo id
    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo mới role
    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        Role savedRole = roleService.save(role);
        return ResponseEntity.ok(savedRole);
    }

    // Cập nhật role
    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role updatedRole) {
        return roleService.getRoleById(id)
                .map(existing -> {
                    updatedRole.setRoleId(id);
                    return ResponseEntity.ok(roleService.save(updatedRole));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Xóa role
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .map(existing -> {
                    roleService.deleteRole(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
