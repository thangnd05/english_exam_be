package com.example.test.services;

import com.example.test.models.Memberships;

import com.example.test.respositories.MembershipRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MembershipService {



    @Autowired
    private MembershipRepo membershipRepository;

    public ResponseEntity<List<Memberships>>getAllMembership(){
        List<Memberships>memberships=membershipRepository.findAll();
        return new ResponseEntity<>(memberships,HttpStatus.OK);
    }

    public String findNameById(Long id) {
        return membershipRepository.findById(id)
                .map(Memberships::getName)
                .orElseThrow(() -> new RuntimeException("Membership not found with id: " + id));
    }



    public ResponseEntity<Memberships> getMembershipById(Long id) {
        Optional<Memberships> membership = membershipRepository.findById(id);
        return membership.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found with id: " + id));
    }

    @Transactional
    public void deleteMembership(Long id){
        membershipRepository.deleteById(id);
    }


    @Transactional
    public Memberships saveMembership(Memberships memberships) {
        if (memberships.getMembership_id() == null) {
            memberships.setCreated_at(LocalDateTime.now());
        }
        return membershipRepository.save(memberships);
    }


    @Transactional
    public Memberships updateMemberShip(Long id, Memberships memberships){
        return membershipRepository.findById(id).map(memberUpdate -> {
            memberUpdate.setName(memberships.getName());
            return membershipRepository.save(memberUpdate); // Lưu và trả về bản ghi đã cập nhật
        }).orElseThrow(() ->new RuntimeException("Not Found with id:" + id));
    }
}