package com.example.english_exam.repositories;

import com.example.english_exam.models.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    boolean existsByName(String name);
}
