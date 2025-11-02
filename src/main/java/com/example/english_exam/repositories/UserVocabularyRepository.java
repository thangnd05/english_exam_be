package com.example.english_exam.repositories;

import com.example.english_exam.models.UserVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {
    Optional<UserVocabulary> findByUserIdAndVocabId(Long userId, Long vocabId);

    @Query("SELECT uv FROM UserVocabulary uv " +
            "JOIN Vocabulary v ON uv.vocabId = v.vocabId " +
            "WHERE uv.userId = :userId " +
            "AND v.albumId = :albumId " +
            "AND uv.status <> :status")
    List<UserVocabulary> findByUserIdAndAlbumIdAndStatusNot(
            @Param("userId") Long userId,
            @Param("albumId") Long albumId,
            @Param("status") UserVocabulary.Status status
    );

    // Nếu muốn filter cả album
    @Query("SELECT uv.vocabId FROM UserVocabulary uv WHERE uv.userId = :userId AND uv.status = 'mastered' AND uv.vocabId IN (SELECT v.vocabId FROM Vocabulary v WHERE v.albumId = :albumId)")
    List<Long> findMasteredVocabIdsByUserIdAndAlbumId(@Param("userId") Long userId, @Param("albumId") Long albumId);


    @Query("SELECT uv.vocabId FROM UserVocabulary uv WHERE uv.userId = :userId AND uv.status = :status")
    List<Long> findVocabIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserVocabulary.Status status);

}
