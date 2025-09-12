package com.example.test.respositories;


import com.example.test.models.Memberships;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembershipRepo extends JpaRepository<Memberships,Long> {





}
