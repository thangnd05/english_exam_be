package com.example.test.controller;


import com.example.test.models.Memberships;
import com.example.test.services.MembershipService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/memberships")
@Validated
public class MembershipController {

    @Autowired
    private MembershipService membershipService;


    @GetMapping
    public ResponseEntity<List<Memberships>> getAllMemberships() {
        return membershipService.getAllMembership();
    }


    @GetMapping("/{id}")
    public ResponseEntity<Memberships> getMembershipById(@PathVariable Long id) {
        return membershipService.getMembershipById(id);
    }


    @GetMapping("/{id}/name")
    public String getMembershipNameById(@PathVariable Long id) {
        return membershipService.findNameById(id);
    }


    @PostMapping
    public ResponseEntity<Memberships> createMembership(@Valid @RequestBody Memberships membership) {
        Memberships savedMembership = membershipService.saveMembership(membership);
        return new ResponseEntity<>(savedMembership, HttpStatus.CREATED);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Memberships> updateMembership(@PathVariable Long id, @Valid @RequestBody Memberships membership) {
        Memberships updatedMembership = membershipService.updateMemberShip(id, membership);
        return new ResponseEntity<>(updatedMembership, HttpStatus.OK);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembership(@PathVariable Long id) {
        membershipService.deleteMembership(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
