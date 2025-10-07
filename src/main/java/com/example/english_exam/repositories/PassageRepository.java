package com.example.english_exam.repositories;

import com.example.english_exam.models.Passage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassageRepository extends JpaRepository<Passage, Long> {
    @Query(value = "SELECT * FROM passages WHERE passage_type = :type ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Passage> findRandomPassages(@Param("type") String type, @Param("limit") int limit);
}
