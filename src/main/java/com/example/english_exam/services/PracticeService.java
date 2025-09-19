package com.example.english_exam.services;

import com.example.english_exam.dto.request.PracticeAnswerRequest;
import com.example.english_exam.dto.request.PracticeOptionRequest;
import com.example.english_exam.dto.request.PracticeQuestionRequest;
import com.example.english_exam.dto.response.PracticeAnswerResponse;
import com.example.english_exam.dto.response.PracticeOptionResponse;
import com.example.english_exam.dto.response.PracticeQuestionResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PracticeService {

    @Autowired
    private PracticeQuestionRepository questionRepository;

    @Autowired
    private PracticeOptionRepository optionRepository;

    @Autowired
    private PracticeAnswerRepository answerRepository;

    public PracticeQuestionResponse createPracticeQuestion(PracticeQuestionRequest request) {

        // 1. Tạo PracticeQuestion
        PracticeQuestion question = new PracticeQuestion();
        question.setVocabId(request.getVocabId());
        question.setType(request.getType());
        question.setQuestionText(request.getQuestionText());
        question.setAudioUrl(request.getAudioUrl());
        question = questionRepository.save(question);

        List<PracticeOption> optionEntities = new ArrayList<>();
        PracticeAnswer answerEntity = null;

        // 2. Tạo đáp án dựa trên type
        switch (question.getType()) {

            case MULTICHOICE:
                // Tạo nhiều lựa chọn
                if (request.getOptions() != null) {
                    for (PracticeOptionRequest or : request.getOptions()) {
                        PracticeOption option = new PracticeOption();
                        option.setPracticeQuestionId(question.getId());
                        option.setOptionText(or.getOptionText());
                        option.setCorrect(or.isCorrect());
                        optionEntities.add(optionRepository.save(option));
                    }
                }
                break;

            case LISTENING_EN:
            case LISTENING_VI:
            case WRITING_EN:
            case WRITING_VI:
                // Tạo đáp án chuẩn
                PracticeAnswerRequest ar = request.getAnswer();
                if (ar != null) {
                    answerEntity = new PracticeAnswer();
                    answerEntity.setPracticeQuestionId(question.getId());
                    answerEntity.setCorrectEnglish(ar.getCorrectEnglish());
                    answerEntity.setCorrectVietnamese(ar.getCorrectVietnamese());
                    answerEntity = answerRepository.save(answerEntity);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown practice question type: " + question.getType());
        }

        // 3. Chuyển sang DTO response
        List<PracticeOptionResponse> optionResponses = new ArrayList<>();
        for (PracticeOption o : optionEntities) {
            optionResponses.add(new PracticeOptionResponse(o.getId(), o.getOptionText(), o.isCorrect()));
        }

        PracticeAnswerResponse answerResponse = null;
        if (answerEntity != null) {
            answerResponse = new PracticeAnswerResponse(
                    answerEntity.getId(),
                    answerEntity.getCorrectEnglish(),
                    answerEntity.getCorrectVietnamese()
            );
        }

        return new PracticeQuestionResponse(
                question.getId(),
                question.getVocabId(),
                question.getType(),
                question.getQuestionText(),
                question.getAudioUrl(),
                optionResponses,
                answerResponse
        );
    }

    public List<PracticeQuestionResponse> getAllPracticeQuestions() {
        return questionRepository.findAll().stream()
                .map(q -> {
                    // load options
                    List<PracticeOptionResponse> optionResponses = optionRepository.findAll().stream()
                            .filter(o -> o.getPracticeQuestionId().equals(q.getId()))
                            .map(o -> new PracticeOptionResponse(o.getId(), o.getOptionText(), o.isCorrect()))
                            .collect(Collectors.toList());
                    // load answer
                    PracticeAnswerResponse answerResponse = answerRepository.findAll().stream()
                            .filter(a -> a.getPracticeQuestionId().equals(q.getId()))
                            .map(a -> new PracticeAnswerResponse(a.getId(), a.getCorrectEnglish(), a.getCorrectVietnamese()))
                            .findFirst()
                            .orElse(null);
                    return new PracticeQuestionResponse(
                            q.getId(),
                            q.getVocabId(),
                            q.getType(),
                            q.getQuestionText(),
                            q.getAudioUrl(),
                            optionResponses,
                            answerResponse
                    );
                })
                .collect(Collectors.toList());
    }

    public Optional<PracticeQuestionResponse> getPracticeQuestionById(Long id) {
        return questionRepository.findById(id).map(q -> {
            List<PracticeOptionResponse> optionResponses = optionRepository.findAll().stream()
                    .filter(o -> o.getPracticeQuestionId().equals(q.getId()))
                    .map(o -> new PracticeOptionResponse(o.getId(), o.getOptionText(), o.isCorrect()))
                    .collect(Collectors.toList());

            PracticeAnswerResponse answerResponse = answerRepository.findAll().stream()
                    .filter(a -> a.getPracticeQuestionId().equals(q.getId()))
                    .map(a -> new PracticeAnswerResponse(a.getId(), a.getCorrectEnglish(), a.getCorrectVietnamese()))
                    .findFirst()
                    .orElse(null);

            return new PracticeQuestionResponse(
                    q.getId(),
                    q.getVocabId(),
                    q.getType(),
                    q.getQuestionText(),
                    q.getAudioUrl(),
                    optionResponses,
                    answerResponse
            );
        });
    }
}
