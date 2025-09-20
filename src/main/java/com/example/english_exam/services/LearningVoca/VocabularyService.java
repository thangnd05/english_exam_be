package com.example.english_exam.services.LearningVoca;


import com.example.english_exam.dto.request.VocabularyRequest;
import com.example.english_exam.dto.response.VocabularyResponse;
import com.example.english_exam.models.DictionaryResult;
import com.example.english_exam.models.Vocabulary;
import com.example.english_exam.models.VocabularyAlbum;
import com.example.english_exam.repositories.VocabularyAlbumRepository;
import com.example.english_exam.repositories.VocabularyRepository;
import com.example.english_exam.services.ApiExtend.DictionaryApiService;
import com.example.english_exam.services.ApiExtend.TextToSpeechService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VocabularyService {
    private final VocabularyRepository repository;
    private final VocabularyAlbumRepository albumRepository;
    private final DictionaryApiService dictionaryApiService;
    private final TextToSpeechService textToSpeechService;

    public VocabularyService(VocabularyRepository repository, VocabularyAlbumRepository albumRepository, DictionaryApiService dictionaryApiService, TextToSpeechService textToSpeechService) {
        this.repository = repository;
        this.albumRepository = albumRepository;
        this.dictionaryApiService = dictionaryApiService;
        this.textToSpeechService = textToSpeechService;
    }

    public List<Vocabulary> findAll() {
        return repository.findAll();
    }

    public Optional<Vocabulary> findById(Long id) {
        return repository.findById(id);
    }

    public Vocabulary save(Vocabulary vocab) {
        return repository.save(vocab);
    }

    public boolean delete(Long id) {
        return repository.findById(id).map(v -> {
            repository.delete(v);
            return true;
        }).orElse(false);
    }

    public VocabularyResponse createVocabulary(VocabularyRequest request) {
        VocabularyAlbum album;

        if (request.getAlbumId() != null) {
            album = albumRepository.findById(request.getAlbumId())
                    .orElseThrow(() -> new RuntimeException("Album không tồn tại"));
        } else if (request.getNewAlbumName() != null && !request.getNewAlbumName().isEmpty()) {
            album = new VocabularyAlbum();
            album.setName(request.getNewAlbumName());
            album.setDescription(request.getNewAlbumDesc());
            album.setUserId(request.getUserId());
            album = albumRepository.save(album);
        } else {
            throw new RuntimeException("Phải chọn album sẵn có hoặc nhập tên album mới");
        }

        Vocabulary vocab = new Vocabulary();
        vocab.setWord(request.getWord());
        vocab.setMeaning(request.getMeaning());
        vocab.setExample(request.getExample());
        vocab.setAlbumId(album.getAlbumId());

        // 👇 gọi API Dictionary
        DictionaryResult result = dictionaryApiService.fetchWordInfo(request.getWord());
        if (result != null) {
            vocab.setPhonetic(result.getPhonetic());

            if (result.getAudioUrl() != null && !result.getAudioUrl().isEmpty()) {
                vocab.setVoiceUrl(result.getAudioUrl());
            } else {
                // fallback sang Google TTS
                String ttsUrl = textToSpeechService.generateAudio(request.getWord());
                vocab.setVoiceUrl(ttsUrl);
            }
        } else {
            // fallback cả phonetic và audio nếu Dictionary API fail
            String ttsUrl = textToSpeechService.generateAudio(request.getWord());
            vocab.setVoiceUrl(ttsUrl);
        }

        vocab = repository.save(vocab);

        VocabularyResponse response = new VocabularyResponse();
        response.setVocabId(vocab.getVocabId());
        response.setWord(vocab.getWord());
        response.setPhonetic(vocab.getPhonetic());
        response.setMeaning(vocab.getMeaning());
        response.setExample(vocab.getExample());
        response.setAlbumId(album.getAlbumId());
        response.setAlbumName(album.getName());
        response.setAlbumDesc(album.getDescription());
        response.setVoiceUrl(vocab.getVoiceUrl());
        response.setCreatedAt(vocab.getCreatedAt());

        return response;
    }
}

