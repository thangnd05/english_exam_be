package com.example.english_exam.services;

import com.example.english_exam.models.VocabularyAlbum;
import com.example.english_exam.repositories.VocabularyAlbumRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VocabularyAlbumService {
    private final VocabularyAlbumRepository repository;

    public VocabularyAlbumService(VocabularyAlbumRepository repository) {
        this.repository = repository;
    }

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
}
