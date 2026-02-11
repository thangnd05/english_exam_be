package com.example.english_exam.dto.request;


import lombok.Data;

import java.util.List;
@Data
public class BulkPassageGroupRequest {

    private Long examPartId;
    private Long classId;
    private Long chapterId;

    private List<PassageQuestionGroup> groups;
}

