package com.example.english_exam.services;

import com.example.english_exam.dto.request.ChapterRequest;
import com.example.english_exam.dto.response.ChapterResponse;
import com.example.english_exam.models.Chapter;
import com.example.english_exam.models.ClassEntity;
import com.example.english_exam.repositories.ChapterRepository;
import com.example.english_exam.repositories.ClassRepository;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final ClassRepository classRepository;
    private final AuthUtils authUtils;


    private void checkTeacherPermission(Long classId, Long currentUserId) {

        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (!clazz.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("Forbidden: You are not the teacher of this class");
        }
    }

    // ============================
    // ✅ Helper convert Entity → DTO
    // ============================
    private ChapterResponse toResponse(Chapter c) {
        return new ChapterResponse(
                c.getChapterId(),
                c.getClassId(),
                c.getTitle(),
                c.getDescription(),
                c.getCreatedAt()
        );
    }

    // ============================
    // ✅ CREATE
    // ============================
    public ChapterResponse create(HttpServletRequest httpRequest, ChapterRequest request) {

        Long currentUserId = authUtils.getUserId(httpRequest);
        checkTeacherPermission(request.getClassId(), currentUserId);

        Chapter chapter = new Chapter();
        chapter.setClassId(request.getClassId());
        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        chapter.setCreatedAt(LocalDateTime.now());

        Chapter saved = chapterRepository.save(chapter);

        return toResponse(saved);
    }

    // ============================
    // ✅ GET ALL
    // ============================
    public List<ChapterResponse> getAll() {
        return chapterRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================
    // ✅ GET BY CLASS
    // ============================
    public List<ChapterResponse> getByClassId(Long classId) {
        return chapterRepository.findByClassId(classId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ============================
    // ✅ GET BY ID
    // ============================
    public ChapterResponse getById(Long chapterId) {

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        return toResponse(chapter);
    }

    // ============================
    // ✅ UPDATE
    // ============================
    public ChapterResponse update(HttpServletRequest httpRequest,
                                  Long chapterId,
                                  ChapterRequest request) {

        Long currentUserId = authUtils.getUserId(httpRequest);

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        // ✅ Check teacher quyền của class hiện tại
        checkTeacherPermission(chapter.getClassId(), currentUserId);

        if (request.getClassId() != null &&
                !request.getClassId().equals(chapter.getClassId())) {
            throw new RuntimeException("You cannot change classId of a chapter");
        }

        // ✅ Update allowed fields only
        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());

        return toResponse(chapterRepository.save(chapter));
    }



    // ============================
    // ✅ DELETE
    // ============================
    public void delete(HttpServletRequest httpRequest, Long chapterId) {

        Long currentUserId = authUtils.getUserId(httpRequest);

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        // ✅ check teacher
        checkTeacherPermission(chapter.getClassId(), currentUserId);

        chapterRepository.deleteById(chapterId);
    }

}
