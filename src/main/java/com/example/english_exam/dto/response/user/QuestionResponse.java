package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageMediaResponse;
import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private Long questionId;
    private Long examPartId;
    private String questionText;
    private Question.QuestionType questionType;
    private Boolean isBank;
    private PassageResponse passage;
    private List<PassageMediaResponse> passageMedia;
    private Long testPartId;
    private List<AnswerResponse> answers;


}

