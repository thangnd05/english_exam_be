package com.example.english_exam.services.ExamAndTest;

import com.example.english_exam.cloudinary.CloudinaryService;
import com.example.english_exam.dto.request.*;
import com.example.english_exam.dto.response.*;
import com.example.english_exam.dto.response.admin.AnswerAdminResponse;
import com.example.english_exam.dto.response.admin.QuestionAdminResponse;
import com.example.english_exam.dto.response.user.AnswerResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import com.example.english_exam.models.*;
import com.example.english_exam.repositories.*;
import com.example.english_exam.security.AuthService;
import com.example.english_exam.services.ApiExtend.GeminiService;
import com.example.english_exam.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final GeminiService geminiService;
    private final TestQuestionRepository testQuestionRepository;
    private final PassageRepository  passageRepository;
    private final ExamPartRepository examPartRepository;
    private final CloudinaryService  cloudinaryService;
    private final AuthUtils authUtils;



    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    public Question save(Question question) {
        return questionRepository.save(question);
    }

    public List<QuestionResponse> getQuestionsByPart(Long examPartId, Long classId) {
        List<Question> questions;

        // 🟢 Nếu có classId thì chỉ lấy câu hỏi trong lớp đó
        if (classId != null) {
            questions = questionRepository.findByExamPartIdAndClassId(examPartId, classId);
        } else {
            questions = questionRepository.findByExamPartId(examPartId);
        }

        List<QuestionResponse> responses = new ArrayList<>();
        for (Question q : questions) {
            QuestionResponse dto = new QuestionResponse();
            dto.setQuestionId(q.getQuestionId());
            dto.setExamPartId(q.getExamPartId());
            dto.setQuestionText(q.getQuestionText());
            dto.setQuestionType(q.getQuestionType());
            dto.setExplanation(q.getExplanation());

            List<Answer> answers = answerRepository.findByQuestionId(q.getQuestionId());
            List<AnswerResponse> answerDtos = answers.stream()
                    .map(a -> new AnswerResponse(a.getAnswerId(), a.getAnswerText(), a.getAnswerLabel()))
                    .collect(Collectors.toList());

            dto.setAnswers(answerDtos);
            responses.add(dto);
        }

        return responses;
    }



    public void deleteById(Long id) {
        questionRepository.deleteById(id);
    }

    public long countByExamPartId(Long examPartId, Long classId) {
        if (classId != null) {
            // 🟢 Đếm theo lớp nếu có
            return questionRepository.countByExamPartIdAndClassId(examPartId, classId);
        } else {
            // 🟢 Không có lớp → đếm toàn bộ
            return questionRepository.countByExamPartId(examPartId);
        }
    }


    public QuestionAdminResponse getQuestionDetailAdmin(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // 🔹 Lấy examTypeId qua examPart
        Long examTypeId = examPartRepository.findById(question.getExamPartId())
                .map(p -> p.getExamTypeId())
                .orElse(null);

        // 🔹 Lấy passage
        PassageResponse passageDto = null;
        if (question.getPassageId() != null) {
            Passage p = passageRepository.findById(question.getPassageId())
                    .orElse(null);
            if (p != null) {
                passageDto = new PassageResponse(
                        p.getPassageId(),
                        p.getContent(),
                        p.getMediaUrl(),
                        p.getPassageType()
                );
            }
        }

        // 🔹 Lấy danh sách đáp án
        List<AnswerAdminResponse> answers = answerRepository.findByQuestionId(questionId)
                .stream()
                .map(a -> new AnswerAdminResponse(
                        a.getAnswerId(),
                        a.getAnswerText(),
                        a.getIsCorrect(),
                        a.getAnswerLabel()
                ))
                .toList();

        // 🔹 Build DTO trả ra
        return new QuestionAdminResponse(
                question.getQuestionId(),
                examTypeId,
                question.getExamPartId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getExplanation(),
                passageDto,
                null, // testPartId
                answers,
                question.getClassId()
        );

    }




}
