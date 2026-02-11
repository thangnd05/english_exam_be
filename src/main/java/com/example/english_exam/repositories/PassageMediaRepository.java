package com.example.english_exam.repositories;

import com.example.english_exam.models.PassageMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PassageMediaRepository extends JpaRepository<PassageMedia, Long> {

    List<PassageMedia> findByPassageId(Long passageId);

    void deleteByPassageId(Long passageId);
}
