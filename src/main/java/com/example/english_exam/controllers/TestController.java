package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.TestRequest;
import com.example.english_exam.dto.response.admin.TestAdminResponse;
import com.example.english_exam.dto.response.user.TestResponse;
import com.example.english_exam.models.Test;
import com.example.english_exam.services.ExamAndTest.TestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestService testService;
    private final ObjectMapper objectMapper; // <-- 1. Khai báo một field final


    public TestController(TestService testService, ObjectMapper objectMapper) {
        this.testService = testService;
        this.objectMapper = objectMapper;
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

    @GetMapping("/usertest/{testId}")
    public ResponseEntity<TestResponse> getUserTest(
            @PathVariable Long testId,
            @RequestParam Long userId
    ) {
        try {
            // Gọi service, truyền cả userId để tính attempts, remaining...
            TestResponse response = testService.getTestFullById(testId, userId);

            if (response == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Ví dụ test không tồn tại
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            // Lỗi khác
            return ResponseEntity.status(500).body(null);
        }
    }


    @GetMapping("/admintest/{testId}")
    public ResponseEntity<TestAdminResponse> getTestByIdAdmin(@PathVariable Long testId) {
        TestAdminResponse response = testService.getTestFullByIdAdmin(testId);
        return ResponseEntity.ok(response);
    }


    // Tạo test mới

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestResponse> createTest(
            @RequestParam("data") String dataJson,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile
    ) throws IOException {

        // 3. DÙNG ObjectMapper đã được inject, KHÔNG tạo mới
        TestRequest request = this.objectMapper.readValue(dataJson, TestRequest.class);

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

    @PostMapping(value = "/pratise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TestResponse createPracticeTest(
            @RequestParam("data") String dataJson,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile
    ) throws IOException {
        // parse JSON sang DTO
        ObjectMapper mapper = new ObjectMapper();
        TestRequest request = mapper.readValue(dataJson, TestRequest.class);

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

    // Lấy danh sách test theo examTypeId cho user
    @GetMapping("/user/by-exam-type/{examTypeId}")
    public ResponseEntity<List<TestResponse>> getTestsByExamTypeForUser(
            @PathVariable Long examTypeId,
            @RequestParam Long userId
    ) {
        // Lấy tất cả test của admin
        List<Test> tests = testService.getAllTestsByAdmin()
                .stream()
                .filter(t -> t.getExamTypeId().equals(examTypeId))
                .toList();

        // Chuyển sang TestResponse, tính lượt còn lại cho user
        List<TestResponse> responses = tests.stream()
                .map(t -> testService.getTestFullById(t.getTestId(), userId))
                .toList();

        return ResponseEntity.ok(responses);
    }



    @GetMapping("/{testId}/can-start")
    public ResponseEntity<Map<String, Object>> canStartTest(
            @PathVariable Long testId,
            @RequestParam Long userId
    ) {
        Test test = testService.getTestById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        Map<String, Object> result = testService.canStartTest(userId, test);

        if (!(Boolean) result.get("canStart")) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }








}
