package com.example.english_exam.dto.response.admin;

import com.example.english_exam.dto.response.PassageResponse;
import com.example.english_exam.dto.response.user.QuestionResponse;
import com.example.english_exam.models.Question;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionAdminResponse extends QuestionResponse {

    private Long examTypeId;
    private Long classId;

    /** true = câu trong kho; false = câu tạo gắn thẳng đề */
    private Boolean isBank;

    public QuestionAdminResponse(
            Long questionId,
            Long examTypeId,
            Long examPartId,
            String questionText,
            Question.QuestionType questionType,
            String explanation,
            Long testPartId,
            List<AnswerAdminResponse> answers,
            Long classId,
            Boolean isBank
    ) {

        super(
                questionId,
                examPartId,
                questionText,
                questionType,
                explanation,
                testPartId,
                answers != null ? new ArrayList<>(answers) : null
        );

        this.examTypeId = examTypeId;
        this.classId = classId;
        this.isBank = isBank;
    }
}
