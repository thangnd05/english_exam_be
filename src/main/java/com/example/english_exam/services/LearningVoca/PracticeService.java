package com.example.english_exam.services.LearningVoca;

import com.example.english_exam.dto.request.PracticeAnswerRequest;
import com.example.english_exam.dto.request.PracticeQuestionRequest;
import com.example.english_exam.dto.response.PracticeAnswerResponse;
import com.example.english_exam.dto.response.PracticeOptionResponse;
import com.example.english_exam.dto.response.PracticeQuestionResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PracticeService {

    @Autowired
    private PracticeQuestionRepository questionRepository;

    @Autowired
    private PracticeOptionRepository optionRepository;

    @Autowired
    private PracticeAnswerRepository answerRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private PracticeQuestionRepository practiceQuestionRepository;
    @Autowired
    private PracticeOptionRepository practiceOptionRepository;
    @Autowired
    private UserVocabularyRepository  userVocabularyRepository;

    private final AuthUtils  authUtils;

    // ============================= CRUD CƠ BẢN =============================

    public PracticeQuestionResponse createPracticeQuestion(PracticeQuestionRequest request) {
        PracticeQuestion question = new PracticeQuestion();
        question.setVocabId(request.getVocabId());
        question.setType(request.getType());
        question.setQuestionText(request.getQuestionText());

        PracticeQuestion saved = questionRepository.save(question);

        List<PracticeOptionResponse> optionResponses = null;
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            List<PracticeOption> options = request.getOptions().stream()
                    .map(optReq -> new PracticeOption(null, saved.getId(),
                            optReq.getOptionText(), optReq.isCorrect()))
                    .collect(Collectors.toList());

            List<PracticeOption> savedOptions = optionRepository.saveAll(options);

            optionResponses = savedOptions.stream()
                    .map(opt -> new PracticeOptionResponse(opt.getId(), opt.getOptionText()))
                    .collect(Collectors.toList());
        }

        PracticeAnswerResponse answerResponse = null;
        if (request.getAnswer() != null) {
            PracticeAnswerRequest ansReq = request.getAnswer();
            PracticeAnswer answer = new PracticeAnswer();
            answer.setPracticeQuestionId(saved.getId());
            answer.setCorrectEnglish(ansReq.getCorrectEnglish());
            answer.setCorrectVietnamese(ansReq.getCorrectVietnamese());

            PracticeAnswer savedAnswer = answerRepository.save(answer);

            answerResponse = new PracticeAnswerResponse(
                    savedAnswer.getCorrectEnglish(),
                    savedAnswer.getCorrectVietnamese()
            );
        }

        // lấy audio từ vocab nếu có
        String audioUrl = vocabularyRepository.findById(saved.getVocabId())
                .map(Vocabulary::getVoiceUrl)
                .orElse(null);

        return new PracticeQuestionResponse(
                saved.getId(),
                saved.getVocabId(),
                saved.getType(),
                saved.getQuestionText(),
                audioUrl,
                optionResponses,
                answerResponse
        );
    }

    public List<PracticeQuestionResponse> getAllPracticeQuestions() {
        return questionRepository.findAll().stream()
                .map(q -> {
                    List<PracticeOptionResponse> optionResponses =
                            optionRepository.findByPracticeQuestionId(q.getId()).stream()
                                    .map(opt -> new PracticeOptionResponse(opt.getId(), opt.getOptionText()))
                                    .collect(Collectors.toList());

                    PracticeAnswerResponse answerResponse = answerRepository.findByPracticeQuestionId(q.getId())
                            .map(ans -> new PracticeAnswerResponse(ans.getCorrectEnglish(), ans.getCorrectVietnamese()))
                            .orElse(null);

                    String audioUrl = vocabularyRepository.findById(q.getVocabId())
                            .map(Vocabulary::getVoiceUrl)
                            .orElse(null);

                    return new PracticeQuestionResponse(
                            q.getId(),
                            q.getVocabId(),
                            q.getType(),
                            q.getQuestionText(),
                            audioUrl,
                            null,   // options
                            null
                    );
                })
                .collect(Collectors.toList());
    }

    public Optional<PracticeQuestionResponse> getPracticeQuestionById(Long id) {
        return questionRepository.findById(id)
                .map(q -> {
                    List<PracticeOptionResponse> optionResponses =
                            optionRepository.findByPracticeQuestionId(q.getId()).stream()
                                    .map(opt -> new PracticeOptionResponse(opt.getId(), opt.getOptionText()))
                                    .collect(Collectors.toList());

                    PracticeAnswerResponse answerResponse = answerRepository.findByPracticeQuestionId(q.getId())
                            .map(ans -> new PracticeAnswerResponse(ans.getCorrectEnglish(), ans.getCorrectVietnamese()))
                            .orElse(null);

                    String audioUrl = vocabularyRepository.findById(q.getVocabId())
                            .map(Vocabulary::getVoiceUrl)
                            .orElse(null);

                    return new PracticeQuestionResponse(
                            q.getId(),
                            q.getVocabId(),
                            q.getType(),
                            q.getQuestionText(),
                            audioUrl,
                            null,   // options
                            null
                    );
                });
    }

    public Optional<PracticeQuestionResponse> generateOneRandomQuestion(HttpServletRequest httpRequest, Long albumId) {

        Long currentUserId = authUtils.getUserId(httpRequest);

        // 1. Lấy danh sách vocabId đã mastered của user trong album
        List<Long> masteredIds = userVocabularyRepository.findMasteredVocabIdsByUserIdAndAlbumId(currentUserId, albumId);

        // 2. Lấy tất cả từ trong album
        List<Vocabulary> vocabularies = vocabularyRepository.findByAlbumId(albumId);

        // 3. Lọc ra những từ chưa mastered
        List<Vocabulary> availableVocabs = vocabularies.stream()
                .filter(v -> !masteredIds.contains(v.getVocabId()))
                .collect(Collectors.toList());

        if (availableVocabs.isEmpty()) return Optional.empty(); // hết từ chưa học

        // 4. Chọn random 1 từ
        Random rand = new Random();
        Vocabulary vocab = availableVocabs.get(rand.nextInt(availableVocabs.size()));

        // 5. Chọn random type
        PracticeQuestion.QuestionType type = rand.nextBoolean()
                ? PracticeQuestion.QuestionType.MULTICHOICE
                : PracticeQuestion.QuestionType.LISTENING_EN;

        // 6. Tạo PracticeQuestion
        PracticeQuestion question = new PracticeQuestion();
        question.setVocabId(vocab.getVocabId());
        question.setType(type);
        question.setQuestionText(type == PracticeQuestion.QuestionType.MULTICHOICE
                ? "Chọn nghĩa đúng của từ: " + vocab.getWord()
                : "Nghe/viết lại từ: " + vocab.getMeaning());

        PracticeQuestion saved = questionRepository.save(question);

        // 7. Tạo options/answer
        List<PracticeOptionResponse> optionResponses = null;
        PracticeAnswerResponse answerResponse = null;

        if (type == PracticeQuestion.QuestionType.MULTICHOICE) {
            List<Vocabulary> distractors = vocabularies.stream()
                    .filter(v -> !v.getVocabId().equals(vocab.getVocabId()))
                    .limit(3)
                    .collect(Collectors.toList());

            List<PracticeOption> options = new ArrayList<>();
            options.add(new PracticeOption(null, saved.getId(), vocab.getMeaning(), true));
            for (Vocabulary d : distractors) {
                options.add(new PracticeOption(null, saved.getId(), d.getMeaning(), false));
            }

            List<PracticeOption> savedOptions = practiceOptionRepository.saveAll(options);
            optionResponses = savedOptions.stream()
                    .map(o -> new PracticeOptionResponse(o.getId(), o.getOptionText()))
                    .collect(Collectors.toList());
        } else {
            PracticeAnswer ans = new PracticeAnswer();
            ans.setPracticeQuestionId(saved.getId());
            ans.setCorrectEnglish(vocab.getWord());
            ans.setCorrectVietnamese(vocab.getMeaning());
            PracticeAnswer savedAns = answerRepository.save(ans);

            answerResponse = new PracticeAnswerResponse(
                    savedAns.getCorrectEnglish(),
                    savedAns.getCorrectVietnamese()
            );
        }

        return Optional.of(new PracticeQuestionResponse(
                saved.getId(),
                saved.getVocabId(),
                saved.getType(),
                saved.getQuestionText(),
                vocab.getVoiceUrl(),
                optionResponses,
                answerResponse
        ));
    }



}
