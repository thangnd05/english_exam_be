package com.example.english_exam.services.LearningVoca;

import com.example.english_exam.dto.request.PracticeCheckRequest;
import com.example.english_exam.dto.response.PracticeCheckResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PracticeCheckService {

    private final PracticeQuestionRepository practiceQuestionRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final PracticeOptionRepository practiceOptionRepository;
    private final AuthUtils  authUtils;

    public PracticeCheckResponse checkAnswer(PracticeCheckRequest request, HttpServletRequest httpRequest) {

        Long currentUserId = authUtils.getUserId(httpRequest);

        PracticeQuestion question = practiceQuestionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

        boolean correct;

        if (question.getType() == PracticeQuestion.QuestionType.MULTICHOICE) {
            // Kiểm tra option được chọn
            PracticeOption option = practiceOptionRepository.findById(request.getSelectedOptionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy option đã chọn"));
            correct = option.isCorrect();

        } else { // LISTENING_EN
            Vocabulary vocab = vocabularyRepository.findById(question.getVocabId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy từ vựng"));
            boolean correctEn = vocab.getWord().equalsIgnoreCase(request.getUserEnglish());
            boolean correctVi = vocab.getMeaning().equalsIgnoreCase(request.getUserVietnamese());
            correct = correctEn && correctVi;
        }

        // Lấy hoặc tạo UserVocabulary
        UserVocabulary uv = userVocabularyRepository
                .findByUserIdAndVocabId(currentUserId, question.getVocabId())
                .orElse(new UserVocabulary(null, currentUserId, question.getVocabId(),
                        UserVocabulary.Status.learning, LocalDateTime.now(), 0));

        if (correct) {
            uv.setCorrectCount(uv.getCorrectCount() + 1);
            if (uv.getCorrectCount() >= 4) {
                uv.setStatus(UserVocabulary.Status.mastered);
            }
        } else {
            uv.setCorrectCount(0);
        }

        uv.setLastReviewed(LocalDateTime.now());
        userVocabularyRepository.save(uv);

        return new PracticeCheckResponse(
                request.getQuestionId(),
                correct,
                uv.getStatus().name(),
                uv.getCorrectCount()
        );
    }
}






