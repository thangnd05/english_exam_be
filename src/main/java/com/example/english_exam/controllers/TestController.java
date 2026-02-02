package com.example.english_exam.controllers;

import com.example.english_exam.dto.request.CreateTestWithQuestionsRequest;
import com.example.english_exam.dto.request.TestRequest;
import com.example.english_exam.dto.response.admin.TestAdminResponse;
import com.example.english_exam.dto.response.user.TestResponse;
import com.example.english_exam.models.Test;
import com.example.english_exam.services.ExamAndTest.TestService;
import com.example.english_exam.util.AuthUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tests")
@AllArgsConstructor
public class TestController {

    private final TestService testService;
    private final ObjectMapper objectMapper; // <-- 1. Khai báo một field final
    private final AuthUtils authUtils;



    // Lấy tất cả tests
    @GetMapping
    public ResponseEntity<List<Test>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests()); // 200 OK
    }

    // Lấy test theo id
    @GetMapping("/{id}")
    public ResponseEntity<TestAdminResponse> getTestById(@PathVariable Long id) {
        TestAdminResponse response = testService.getTestDetailForAdmin(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }


    @GetMapping("/usertest/{testId}")
    public ResponseEntity<TestResponse> getUserTest(
            @PathVariable Long testId,
            HttpServletRequest httpRequest
    ) {
        try {
            // ✅ Gọi service: userId tự lấy từ token bên trong service
            TestResponse response = testService.getTestFullById(testId, httpRequest);

            if (response == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }



    @GetMapping("/admintest/{testId}")
    public ResponseEntity<TestAdminResponse> getTestByIdAdmin(@PathVariable Long testId) {
        TestAdminResponse response = testService.getTestFullByIdAdmin(testId);
        return ResponseEntity.ok(response);
    }


    // Tạo test mới

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestResponse> createTestFromQuestionBank(
            @RequestParam("data") String dataJson,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile,
            HttpServletRequest httpRequest
    ) throws IOException {

        // ✅ Parse JSON sang DTO
        TestRequest request = objectMapper.readValue(dataJson, TestRequest.class);
        // ✅ Gọi service
        TestResponse response = testService.createTestFromQuestionBank(request, bannerFile,httpRequest);

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
    public ResponseEntity<List<TestResponse>> getTestsByExamType(
            @PathVariable Long examTypeId,
            HttpServletRequest httpRequest
    ) {
        try {
            // ✅ 1. Lấy userId từ token (nếu có)
            Long currentUserId = null;
            try {
                currentUserId = authUtils.getUserId(httpRequest);;
            } catch (Exception ignored) {
                // Nếu token không hợp lệ hoặc không có token, coi như người dùng chưa đăng nhập
            }

            // ✅ 2. Lấy danh sách bài thi theo examType và chỉ lấy test chưa thuộc class nào (classId = null)
            List<Test> tests = testService.getAllTestsByAdmin()
                    .stream()
                    .filter(t -> t.getExamTypeId().equals(examTypeId))
                    .filter(t -> t.getClassId() == null) // 🟢 chỉ lấy test chưa gán lớp
                    .toList();

            List<TestResponse> responses;

            // ✅ 3. Nếu có userId → trả bản đầy đủ
            if (currentUserId != null) {
                responses = tests.stream()
                        .map(test -> testService.getTestFullById(test.getTestId(), httpRequest))
                        .toList();
            } else {
                // ✅ Nếu chưa đăng nhập → trả bản rút gọn
                responses = tests.stream()
                        .map(TestResponse::new)
                        .toList();
            }

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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

    @PostMapping(path = "/create-with-questions", consumes = "multipart/form-data")
    public ResponseEntity<?> createTestWithNewQuestions(
            @RequestParam("testData") String testDataJson,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
            @RequestPart(value = "audioFiles", required = false) List<MultipartFile> audioFiles,
            HttpServletRequest httpRequest // 🆕 thêm dòng này để lấy token từ cookie
    ) {
        try {
            // ✅ Parse JSON thành DTO
            CreateTestWithQuestionsRequest request = objectMapper.readValue(
                    testDataJson,
                    CreateTestWithQuestionsRequest.class
            );

            // ✅ Gọi service và truyền request kèm HttpServletRequest
            TestResponse newTest = testService.createTestWithNewQuestions(
                    request,
                    bannerFile,
                    audioFiles,
                    httpRequest // 🆕 truyền vào để service lấy userId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(newTest);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("❌ Error processing test data: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error creating test: " + e.getMessage());
        }
    }

    @GetMapping("/by-class/{classId}")
    public ResponseEntity<?> getTestsByClass(@PathVariable Long classId, HttpServletRequest request) {
        List<Test> tests = testService.getTestByClassId(classId, request);

        if (tests.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có bài test nào trong lớp này"));
        }

        return ResponseEntity.ok(tests.stream().map(TestResponse::new).toList());
    }

    @GetMapping("/my-test")
    public ResponseEntity<?> getTestsCreateBy(HttpServletRequest request) {
        List<Test> tests = testService.getTestByCreateBy(request);

        if (tests.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Không có bài test nào trong lớp này"));
        }

        return ResponseEntity.ok(tests.stream().map(TestResponse::new).toList());
    }

    @PutMapping(value = "/{testId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestResponse> updateTestFromQuestionBank(
            @PathVariable Long testId,
            @RequestParam("data") String dataJson,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile,
            HttpServletRequest httpRequest
    ) throws IOException {
        TestRequest request = objectMapper.readValue(dataJson, TestRequest.class);
        TestResponse response = testService.updateTestFromQuestionBank(testId, request, bannerFile, httpRequest);
        return ResponseEntity.ok(response);
    }





}
