package com.example.english_exam.services.LearningVoca;

import com.example.english_exam.models.VocabularyAlbum;
import com.example.english_exam.repositories.VocabularyAlbumRepository;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class VocabularyAlbumService {
    private final VocabularyAlbumRepository repository;
    private final AuthUtils authUtils;


    public List<VocabularyAlbum> findAll() {
        return repository.findAll();
    }

    public Optional<VocabularyAlbum> findById(Long id) {
        return repository.findById(id);
    }

    public VocabularyAlbum save(VocabularyAlbum album) {
        return repository.save(album);
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(a -> {
            repository.delete(a);
            return true;
        }).orElse(false);
    }

    public List<VocabularyAlbum> findAllByUserId(HttpServletRequest httpRequest) {

        Long currentUserId = authUtils.getUserId(httpRequest);
        return repository.findAllByUserId(currentUserId);
    }
}
