package com.example.english_exam.services.LearningVoca;

import com.example.english_exam.dto.request.PracticeAnswerRequest;
import com.example.english_exam.dto.request.PracticeQuestionRequest;
import com.example.english_exam.dto.response.PracticeAnswerResponse;
import com.example.english_exam.dto.response.PracticeOptionResponse;
import com.example.english_exam.dto.response.PracticeQuestionResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
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

    // ============================= RANDOM THEO ALBUM =============================

    public List<PracticeQuestionResponse> generatePracticeQuestionsForAlbum(Long albumId) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByAlbumId(albumId);
        int total = vocabularies.size();
        if (total == 0) return Collections.emptyList();

        // Chia đều cho 3 loại
        int perType = total / 2;
        int remainder = total % 2;

        List<PracticeQuestion.QuestionType> types = new ArrayList<>();
        for (int i = 0; i < perType; i++) types.add(PracticeQuestion.QuestionType.MULTICHOICE);
        for (int i = 0; i < perType; i++) types.add(PracticeQuestion.QuestionType.LISTENING_EN);

        Random rand = new Random();
        for (int i = 0; i < remainder; i++) {
            int r = rand.nextInt(3);
            if (r == 0) types.add(PracticeQuestion.QuestionType.MULTICHOICE);
            else if (r == 1) types.add(PracticeQuestion.QuestionType.LISTENING_EN);
        }

        Collections.shuffle(types);

        List<PracticeQuestionResponse> responses = new ArrayList<>();
        for (int i = 0; i < vocabularies.size(); i++) {
            Vocabulary vocab = vocabularies.get(i);
            PracticeQuestion.QuestionType type = types.get(i);

            PracticeQuestion question = new PracticeQuestion();
            question.setVocabId(vocab.getVocabId());
            question.setType(type);
            question.setQuestionText(type == PracticeQuestion.QuestionType.MULTICHOICE
                    ? "Chọn nghĩa đúng của từ: " + vocab.getWord()
                    : "Nghe/viết lại từ: " + vocab.getMeaning());

            PracticeQuestion saved = questionRepository.save(question);

            List<PracticeOptionResponse> optionResponses = null;
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

                List<PracticeOption> savedOptions = optionRepository.saveAll(options);

                optionResponses = savedOptions.stream()
                        .map(o -> new PracticeOptionResponse(o.getId(), o.getOptionText()))
                        .collect(Collectors.toList());
            }

            PracticeAnswerResponse answerResponse = null;
            if (type == PracticeQuestion.QuestionType.LISTENING_EN) {
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

            responses.add(new PracticeQuestionResponse(
                    saved.getId(),
                    saved.getVocabId(),
                    saved.getType(),
                    saved.getQuestionText(),
                    vocab.getVoiceUrl(),
                    optionResponses,
                    answerResponse
            ));
        }

        return responses;
    }
}
