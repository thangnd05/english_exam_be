# Plan: Cơ chế sửa câu hỏi

Tài liệu này mô tả **cơ chế sửa câu hỏi** tương ứng với các **cơ chế tạo câu hỏi** đã có trong hệ thống. Mục tiêu: thống nhất API, ràng buộc và luồng FE.

---

## 1. Tóm tắt cơ chế TẠO câu hỏi (đã có)

| # | Cơ chế | API / Request | Ghi chú |
|---|--------|----------------|--------|
| 1 | **Tạo 1 câu vào kho** | `POST /api/questions` — `QuestionCreateRequest` (JSON) | Có thể kèm passage; luôn lưu kho. |
| 2 | **Bulk không passage** | `POST /api/questions/bulk` — `BulkCreateQuestionsToBankRequest` + multipart (media_x_x) | Nhiều câu độc lập, mỗi câu có thể có ảnh/audio riêng. |
| 3 | **Bulk có passage** | `POST /api/questions/bulk-with-passage` — `BulkQuestionWithPassageRequest` + optional audio | Nhiều câu chung 1 passage (READING/LISTENING). |
| 4 | **Tạo và gắn luôn đề** | `POST /api/questions/create-and-attach` — `CreateQuestionAndAttachRequest` + multipart | Tạo câu vào kho rồi gắn vào 1 `testPartId`. |
| 5 | **Gắn câu từ kho vào part** | `POST /api/tests/parts/questions` — `AddQuestionsToTestRequest` (testPartId, questionIds) | Chỉ tạo `test_questions`; không tạo câu mới. |
| 6 | **Random từ kho vào part** | `POST /api/tests/parts/random-questions` — `AddRandomQuestionsToTestRequest` (testPartId, count, classId?, chapterId?) | Cá nhân: không gửi classId/chapterId; Lớp: gửi classId (+ chapterId). |

Chi tiết: `Question_Create_Logic.md`, `Huong_chon_va_random_cau_hoi_theo_ca_nhan_va_lop_chapter.md`.

---

## 2. Hai hướng SỬA câu hỏi

Sửa câu hỏi chia làm **hai nhóm**:

- **A. Sửa nội dung câu hỏi trong kho** (bảng `questions`, `answers`, `passages`, `passage_media`): nội dung câu, đáp án, passage, ảnh/audio. Ảnh hưởng **mọi đề** đang dùng câu đó (reference theo `questionId`).
- **B. Sửa “câu hỏi trong đề”** (bảng `test_questions`): không đổi nội dung câu trong kho, chỉ đổi **cấu trúc đề** — xóa câu khỏi part, thay câu khác (replace), hoặc đổi thứ tự hiển thị (nếu có).

---

## 3. A. Sửa nội dung câu hỏi trong kho

### 3.1 Phạm vi sửa (field)

- **Question**: `questionText`, `questionType`, `explanation`, `examPartId`, `classId`, `chapterId` (không đổi `createdBy` khi sửa).
- **Answers**: thêm/sửa/xóa đáp án (đúng/sai, nội dung).
- **Passage** (nếu có): `content`, `passageType`, `mediaUrl`; **Passage media**: thêm/xóa/sửa ảnh, audio (multipart khi cần).

### 3.2 Ràng buộc

| Ràng buộc | Mô tả |
|-----------|--------|
| **Quyền sửa** | Chỉ user tạo câu (`questions.created_by = currentUserId`) được sửa; hoặc admin (nếu có role). Câu thuộc lớp (classId != null) có thể áp dụng thêm rule theo lớp. |
| **Đã có user làm bài** | Vẫn **cho phép sửa** nội dung câu trong kho. Dữ liệu bài làm (user_answers) lưu theo `questionId`; lần làm sau sẽ thấy nội dung mới. Không cần lock. |
| **examPartId** | Có thể cho phép đổi part (câu chuyển part khác) hoặc **không** cho đổi để tránh lệch cấu trúc đề — tùy product. |

### 3.3 Cơ chế sửa tương ứng từng loại tạo

| Cơ chế tạo (đã có) | Cơ chế sửa đề xuất | API / Request |
|--------------------|--------------------|----------------|
| **1. Tạo 1 câu (JSON)** | **Sửa 1 câu** — JSON (và optional multipart nếu có media) | `PUT /api/questions/{id}` — `QuestionUpdateRequest` (chỉ gửi field cần đổi; hoặc full giống create). Nếu có ảnh/audio mới: multipart. |
| **2. Bulk không passage** | Sửa từng câu (gọi nhiều lần PUT) hoặc **phase 2**: `PUT /api/questions/bulk** (batch update) | Ưu tiên: 1 câu / 1 request. |
| **3. Bulk có passage** | Sửa từng câu trong nhóm; passage chung: 1 API sửa passage (ví dụ `PUT /api/passages/{id}`) + các câu vẫn PUT từng câu nếu cần | Có thể thêm `PUT /api/passages/{id}` cho nội dung/type/media passage. |
| **4. Tạo và gắn đề** | Sau khi gắn đề, câu đã nằm trong kho → chỉ cần **sửa nội dung** qua `PUT /api/questions/{id}`. Không cần API “sửa trong đề” riêng. | Dùng chung PUT question. |

### 3.4 DTO và API đề xuất

- **QuestionUpdateRequest**  
  - Giống `QuestionCreateRequest` nhưng mọi field **optional** (patch style): chỉ field gửi lên mới được cập nhật.  
  - Hoặc full body (put style): gửi đủ như create, backend ghi đè toàn bộ.  
  - Nếu câu có passage + media: có thể `PUT /api/questions/{id}` dạng **multipart** (request JSON + file ảnh/audio), tương tự create-and-attach.

- **Response**  
  - Trả `QuestionAdminResponse` (giống GET detail và create) để FE cập nhật state.

- **API**  
  - `PUT /api/questions/{id}`  
    - Body: JSON `QuestionUpdateRequest` (patch hoặc full).  
    - Hoặc multipart khi có thay đổi media (request + file).  
  - (Tùy chọn) `PUT /api/passages/{id}`: chỉ sửa passage (content, type, media) dùng khi bulk-with-passage.

### 3.5 Luồng service (QuestionService)

- `updateQuestion(Long questionId, QuestionUpdateRequest request, HttpServletRequest request, Map<String, MultipartFile> files?)`
  - Kiểm tra tồn tại, quyền sửa (createdBy hoặc admin).
  - Nếu request có thay đổi passage: cập nhật `Passage` (và passage_media nếu có file mới).
  - Cập nhật `Question` (chỉ các field gửi lên nếu patch).
  - Xử lý answers: so sánh với answers hiện tại → thêm mới, cập nhật, xóa (soft/hard tùy thiết kế).
  - Trả `QuestionAdminResponse` (build từ Question + Passage + Answers).

---

## 4. B. Sửa “câu hỏi trong đề” (test_questions)

Ở đây **không** sửa nội dung câu trong kho, chỉ sửa **cấu trúc đề**: part nào có câu nào, thứ tự ra sao.

### 4.1 Hiện trạng

- Bảng **test_questions**: `test_question_id`, `test_part_id`, `question_id`.  
- **Chưa có** cột `display_order` / `sort_order` → thứ tự hiện tại phụ thuộc ID hoặc query mặc định.
- **Đã có** API:
  - `DELETE /api/test-questions/{id}`: xóa 1 bản ghi test_question (bỏ câu khỏi part).

### 4.2 Các thao tác cần hỗ trợ

| Thao tác | Mô tả | API đề xuất |
|----------|--------|-------------|
| **Xóa câu khỏi part** | Xóa 1 bản ghi `test_questions` (câu vẫn còn trong kho). | Đã có: `DELETE /api/test-questions/{id}`. |
| **Thay câu (replace)** | Trong 1 part: “thay câu A bằng câu B” → xóa test_question của A, thêm test_question của B (cùng part). | `PATCH /api/test-questions/{testQuestionId}/replace` body `{ "questionId": <id_câu_mới> }` hoặc `PUT /api/tests/parts/{testPartId}/questions/{testQuestionId}` body `{ "questionId": ... }`. |
| **Đổi thứ tự** | Đổi thứ tự hiển thị câu trong part. | Cần thêm cột `display_order` (integer) vào `test_questions`. API: `PATCH /api/tests/parts/{testPartId}/questions/order` body `[ { "testQuestionId": ..., "order": 0 }, ... ]`. |

### 4.3 Ràng buộc (phần B)

- **Replace**: câu mới (`questionId`) phải cùng `examPartId` với part (test_part → exam_part). Có thể kiểm tra trong service.
- **Quyền**: chỉ user tạo đề (test.created_by) hoặc admin mới được xóa/thay/reorder câu trong đề.
- **Đã có user làm bài**: vẫn cho xóa/thay câu trong đề; bài làm cũ lưu theo `user_test_id` + `question_id` — có thể không hiển thị lại câu đã bị xóa khỏi đề khi xem lại bài, hoặc hiển thị “câu đã bị gỡ” tùy product.

### 4.4 Đề xuất triển khai (B)

1. **Giữ** `DELETE /api/test-questions/{id}` (đã có).
2. **Thêm API replace** (ưu tiên):  
   - `PATCH /api/test-questions/{testQuestionId}/replace`  
   - Body: `{ "questionId": <questionId_mới> }`  
   - Service: kiểm tra part, examPartId của câu mới, quyền; xóa (hoặc cập nhật) test_question cũ; thêm test_question mới với cùng part (và order nếu có).
3. **Thứ tự (reorder)** — phase 2:  
   - Migration thêm `display_order` vào `test_questions`.  
   - API: `PATCH /api/tests/parts/{testPartId}/questions/order` body danh sách `{ testQuestionId, order }`.  
   - Service: cập nhật từng bản ghi `display_order` theo thứ tự gửi lên.

---

## 5. Tóm tắt API sửa câu hỏi

| Loại | Method | Endpoint | Body / Ghi chú |
|------|--------|----------|----------------|
| **A. Nội dung kho** | PUT | `/api/questions/{id}` | `QuestionUpdateRequest` (patch hoặc full); optional multipart cho media. |
| **A. Passage (tùy chọn)** | PUT | `/api/passages/{id}` | Sửa passage độc lập (bulk-with-passage). |
| **B. Xóa khỏi part** | DELETE | `/api/test-questions/{id}` | Đã có. |
| **B. Thay câu trong part** | PATCH | `/api/test-questions/{testQuestionId}/replace` | `{ "questionId": <id_câu_mới> }`. |
| **B. Đổi thứ tự (phase 2)** | PATCH | `/api/tests/parts/{testPartId}/questions/order` | `[ { "testQuestionId", "order" }, ... ]`; cần cột `display_order`. |

---

## 6. Thứ tự triển khai gợi ý

1. **QuestionUpdateRequest** + **PUT /api/questions/{id}** (sửa 1 câu trong kho, JSON; chưa cần multipart nếu product chưa cần sửa ảnh/audio).
2. **QuestionService.updateQuestion** (logic patch/full, cập nhật answers + passage nếu có).
3. **PATCH /api/test-questions/{id}/replace** (thay câu trong part) + kiểm tra examPartId, quyền.
4. (Sau) Multipart cho PUT question khi cần sửa media.
5. (Sau) `display_order` + API reorder.

Sau khi bạn xem plan, có thể chỉnh lại (ví dụ bỏ reorder, đổi tên API, hoặc thêm ràng buộc quyền) rồi triển khai từng bước.
