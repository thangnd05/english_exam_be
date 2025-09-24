package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.TestRequest;
import com.example.english_exam.dto.response.TestResponse;
import com.example.english_exam.models.Test;
import com.example.english_exam.models.User;
import com.example.english_exam.services.ExamAndTest.TestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.catalina.Role;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    // Lấy tất cả tests
    @GetMapping
    public ResponseEntity<List<Test>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests()); // 200 OK
    }

    // Lấy test theo id
    @GetMapping("/{id}")
    public ResponseEntity<Test> getTestById(@PathVariable Long id) {
        return testService.getTestById(id)
                .map(ResponseEntity::ok)   // 200 OK
                .orElse(ResponseEntity.notFound().build()); // 404 Not Found
    }

    // Tạo test mới
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestResponse> createTest(
            @RequestParam("data") String dataJson,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile
    ) throws IOException {
        // Convert JSON string sang DTO
        ObjectMapper mapper = new ObjectMapper();
        TestRequest request = mapper.readValue(dataJson, TestRequest.class);

        TestResponse response = testService.createTest(request, bannerFile);
        return ResponseEntity.ok(response);
    }




    // Cập nhật test
    @PutMapping("/{id}")
    public ResponseEntity<Test> updateTest(@PathVariable Long id, @RequestBody Test updatedTest) {
        return testService.getTestById(id)
                .map(existing -> {
                    updatedTest.setTestId(id);
                    return ResponseEntity.ok(testService.save(updatedTest)); // 200 OK
                })
                .orElse(ResponseEntity.notFound().build()); // 404 Not Found
    }

    // Xoá test
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable Long id) {
        return testService.getTestById(id)
                .map(existing -> {
                    testService.deleteTest(id);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    @PostMapping(value = "/pratise", consumes = {"multipart/form-data"})

    public TestResponse createPracticeTest(
            @RequestPart("data") TestRequest request,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile
    ) throws IOException {
        return testService.createTest(request, bannerFile);
    }


    @GetMapping("/admin")
    public List<Test> getAllTestsByAdmin() {
        return testService.getAllTestsByAdmin();
    }

    // Lấy test theo userId cụ thể
    @GetMapping("/user/{userId}")
    public List<Test> getTestsByUser(@PathVariable Long userId) {
        return testService.getTestsByUser(userId);
    }

    // Lấy tất cả tests của Admin theo examTypeId
    /*
    Giải thích từng bước:

Bước 1: testService.getAllTestsByAdmin() - Lấy tất cả test từ service (dành cho admin)
Bước 2: .stream() - Chuyển danh sách thành stream để xử lý functional
Bước 3: .filter(t -> t.getExamTypeId().equals(examTypeId)) - Lọc chỉ giữ lại các Test có examTypeId khớp với tham số
Bước 4: .toList() - Chuyển stream kết quả thành List
     */
    @GetMapping("/admin/by-exam-type/{examTypeId}")
    public ResponseEntity<List<Test>> getAdminTestsByExamType(@PathVariable Long examTypeId) {
        List<Test> adminTests = testService.getAllTestsByAdmin()
                .stream()
                .filter(t -> t.getExamTypeId().equals(examTypeId))
                .toList();
        return ResponseEntity.ok(adminTests);
    }







}
