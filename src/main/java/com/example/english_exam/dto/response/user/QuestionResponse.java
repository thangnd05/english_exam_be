package com.example.english_exam.dto.response.user;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.models.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionResponse {
    protected Long questionId;
    protected Long examPartId;
    protected String questionText;
    protected Question.QuestionType questionType;
    protected String explanation;

    protected Long testPartId;
    protected List<AnswerResponse> answers;
    protected PassageResponse passage;
    /** passage_id của câu hỏi (null = câu độc lập). Dùng để FE nhóm câu theo passage khi chọn. */
    protected Long passageId;

    public QuestionResponse(Long questionId, Long examPartId, String questionText, Question.QuestionType questionType, String explanation, Long testPartId, List<AnswerResponse> answers, PassageResponse passage) {
        this.questionId = questionId;
        this.examPartId = examPartId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.explanation = explanation;
        this.testPartId = testPartId;
        this.answers = answers;
        this.passage = passage;
    }
}

