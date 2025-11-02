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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private PracticeOptionRepository practiceOptionRepository;
    @Autowired
    private UserVocabularyRepository userVocabularyRepository;

    private final AuthUtils authUtils;

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

        // 1️⃣ Lấy danh sách vocab đã mastered
        List<Long> masteredIds = userVocabularyRepository.findMasteredVocabIdsByUserIdAndAlbumId(currentUserId, albumId);

        // 2️⃣ Lấy tất cả vocab trong album
        List<Vocabulary> vocabularies = vocabularyRepository.findByAlbumId(albumId);

        // 3️⃣ Lọc vocab chưa mastered
        List<Vocabulary> availableVocabs = vocabularies.stream()
                .filter(v -> !masteredIds.contains(v.getVocabId()))
                .collect(Collectors.toList());

        if (availableVocabs.isEmpty()) return Optional.empty();

        // 4️⃣ Random 1 vocab
        Random rand = new Random();
        Vocabulary vocab = availableVocabs.get(rand.nextInt(availableVocabs.size()));

        // 5️⃣ Random loại câu hỏi
        PracticeQuestion.QuestionType type = rand.nextBoolean()
                ? PracticeQuestion.QuestionType.MULTICHOICE
                : PracticeQuestion.QuestionType.LISTENING_EN;

        // 6️⃣ Tạo câu hỏi
        PracticeQuestion question = new PracticeQuestion();
        question.setVocabId(vocab.getVocabId());
        question.setType(type);

        if (type == PracticeQuestion.QuestionType.MULTICHOICE) {
            // MULTICHOICE: hiển thị từ tiếng Anh, chọn nghĩa tiếng Việt
            question.setQuestionText("Chọn nghĩa đúng của từ: " + vocab.getWord());
        } else {
            // LISTENING_EN: chỉ nghe, không hiện nghĩa
            question.setQuestionText("Nghe và nhập lại từ đã nghe");
        }

        PracticeQuestion saved = questionRepository.save(question);

        // 7️⃣ Tạo đáp án / options
        List<PracticeOptionResponse> optionResponses = null;
        PracticeAnswerResponse answerResponse = null;

        if (type == PracticeQuestion.QuestionType.MULTICHOICE) {
            // ✅ MULTICHOICE: 1 đáp án đúng + 3 sai
            List<Vocabulary> distractors = vocabularies.stream()
                    .filter(v -> !v.getVocabId().equals(vocab.getVocabId()))
                    .limit(3)
                    .collect(Collectors.toList());

            List<PracticeOption> options = new ArrayList<>();
            options.add(new PracticeOption(null, saved.getId(), vocab.getMeaning(), true));

            for (Vocabulary d : distractors) {
                options.add(new PracticeOption(null, saved.getId(), d.getMeaning(), false));
            }

            Collections.shuffle(options);
            List<PracticeOption> savedOptions = practiceOptionRepository.saveAll(options);
            optionResponses = savedOptions.stream()
                    .map(o -> new PracticeOptionResponse(o.getId(), o.getOptionText()))
                    .collect(Collectors.toList());
        } else {
            // ✅ LISTENING_EN
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

        // 8️⃣ Trả kết quả
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


    public void markWordAsKnown(HttpServletRequest httpRequest, Long vocabId) {
        Long currentUserId = authUtils.getUserId(httpRequest);

        UserVocabulary uv = userVocabularyRepository.findByUserIdAndVocabId(currentUserId, vocabId)
                .orElse(new UserVocabulary(currentUserId, vocabId));

        uv.setStatus(UserVocabulary.Status.mastered);
        uv.setCorrectCount(5);
        uv.setLastReviewed(LocalDateTime.now());

        userVocabularyRepository.save(uv);
    }

}

