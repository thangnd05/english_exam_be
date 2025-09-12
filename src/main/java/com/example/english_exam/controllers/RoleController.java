package com.example.english_exam.controllers;


import com.example.english_exam.models.Roles;
import com.example.english_exam.services.RoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<Roles> getAllRoles() {
        return roleService.getAllRoles();
    }

    @GetMapping("/{id}")
    public Roles getRole(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @PostMapping
    public Roles createRole(@RequestBody Roles role) {
        return roleService.createRole(role);
    }
    @PutMapping("/{id}")
    public Roles updateRole(@PathVariable Long id, @RequestBody Roles roleDetails) {
        return roleService.updateRole(id, roleDetails);
    }

    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }
}
