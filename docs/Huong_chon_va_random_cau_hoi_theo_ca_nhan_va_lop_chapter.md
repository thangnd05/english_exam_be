# Hướng: Chọn câu hỏi & Random câu hỏi theo Part (Cá nhân / Lớp + Chapter)

## 1. Quy ước dữ liệu (database)

Trong bảng **`questions`**:

| Ý nghĩa | `created_by` | `class_id` | `chapter_id` | Ghi chú |
|--------|--------------|-------------|--------------|---------|
| **Cá nhân** (theo tài khoản đang login) | = userId (JWT) | `NULL` | `NULL` | Chỉ câu do chính user tạo, không thuộc lớp |
| **Lớp + Chapter** (thuộc lớp, có thể thuộc chapter) | bất kỳ | có giá trị | `NULL` hoặc có giá trị | Kho câu của lớp; có thể lọc thêm theo chapter |

- **Đề cá nhân**: test có `class_id` = NULL, `chapter_id` = NULL → chỉ được chọn/random từ **kho cá nhân của user đang login**: `created_by = userId (JWT)` **và** `class_id IS NULL` **và** `chapter_id IS NULL` (không lấy hết mọi câu không class/chapter).
- **Đề lớp (có chapter)**: test có `class_id` = X, `chapter_id` = Y → chọn/random từ câu có class_id = X và (nếu cần) chapter_id = Y.

---

## 2. Hiện trạng backend

### 2.1 Lấy danh sách câu theo part (để chọn)

- **API**: `GET /api/questions/by-part/{examPartId}?classId=...`
- **Logic**:
  - Có `classId` → `findByExamPartIdAndClassId` (chỉ câu của lớp đó).
  - **Không** `classId` → `findByExamPartId` → đang lấy **toàn bộ** câu của part (cả cá nhân lẫn lớp).
- **Thiếu**:
  - Không có tham số `chapterId` → không lọc theo chapter khi chọn câu.
  - Khi không gửi classId chưa rõ ràng là “chỉ cá nhân”; hiện tại là “toàn bộ”.

### 2.2 Random câu theo part

- **API**: `POST /api/tests/parts/random-questions`  
  Body: `{ testPartId, count, classId?, chapterId? }`
- **Logic**:
  - Có `classId` + `chapterId` → random theo part + class + chapter.
  - Chỉ có `classId` → random theo part + class.
  - **Không** classId/chapterId → `findRandomByExamPart` → đang lấy **toàn bộ** câu của part (cả cá nhân lẫn lớp).

**Kết luận**: Cần tách rõ “kho cá nhân” (câu của **user đang login**: created_by = userId từ JWT, và class_id NULL, chapter_id NULL) và “kho lớp/chapter” (class_id + chapter_id). Cá nhân **không** lấy hết mọi câu không class/chapter.

---

## 3. Hướng thay đổi backend

### 3.1 Quy ước tham số API

- **Cá nhân**: không gửi `classId` (và không gửi `chapterId`) → backend lấy **userId từ JWT** và chỉ lấy câu **của user đó** (created_by = userId, class_id IS NULL, chapter_id IS NULL). **Không** lấy hết mọi câu không class/chapter.
- **Lớp**: gửi `classId` → lấy câu của lớp; có thể gửi thêm `chapterId` để lọc theo chapter.

### 3.2 QuestionRepository – thêm/sửa method

1. **Cá nhân theo user đăng nhập (list)**  
   - Lấy danh sách câu theo part, chỉ kho cá nhân của user:  
     `findByExamPartIdAndCreatedByAndClassIdIsNullAndChapterIdIsNull(examPartId, createdBy)`  
   - Đếm:  
     `countByExamPartIdAndCreatedByAndClassIdIsNullAndChapterIdIsNull(examPartId, createdBy)`

2. **Cá nhân theo user đăng nhập (random)**  
   - Random từ kho cá nhân của user theo part:  
     `findRandomByExamPartAndCreatedByAndClassIdIsNullAndChapterIdIsNull(examPartId, createdBy, limit)`  
     (điều kiện: created_by = :createdBy AND class_id IS NULL AND chapter_id IS NULL)

3. **Lớp + Chapter (list)**  
   - Đã có: `findByExamPartIdAndClassId`.  
   - Thêm (nếu cần lọc chapter):  
     `findByExamPartIdAndClassIdAndChapterId(examPartId, classId, chapterId)`  
     và overload: `findByExamPartIdAndClassIdAndChapterId(examPartId, classId, null)` = chỉ class, không lọc chapter.

4. **Lớp + Chapter (count)**  
   - Thêm: `countByExamPartIdAndClassIdAndChapterId` (chapterId nullable).

### 3.3 QuestionService – lấy danh sách & đếm theo part

- **getQuestionsByPart(examPartId, classId, chapterId, currentUserId)** (currentUserId lấy từ JWT trong controller):
  - `classId == null` → gọi repository **cá nhân theo user**: created_by = currentUserId, class_id NULL, chapter_id NULL.
  - `classId != null` → gọi repository **lớp** (có chapterId thì lọc thêm chapter).
- **countByExamPartId(examPartId, classId, chapterId, currentUserId)**:
  - Tương tự: null = cá nhân (theo currentUserId), có classId = lớp (+ chapter nếu có).

### 3.4 QuestionController – API lấy câu theo part

- `GET /api/questions/by-part/{examPartId}` (cần gửi JWT)
  - Query: `classId` (optional), `chapterId` (optional).
  - Không gửi classId → danh sách câu **cá nhân của user đang login** theo part (created_by = userId từ JWT).
  - Gửi classId (và optional chapterId) → danh sách câu **lớp/chapter** theo part.
- `GET /api/questions/count/by-part/{examPartId}` (cần gửi JWT)
  - Cùng query; cá nhân = đếm theo currentUserId.

### 3.5 TestService – random theo part

- **addRandomQuestionsToTestPart(request, currentUserId)** (currentUserId lấy từ JWT trong controller):
  - **Không** gửi classId (và chapterId) → gọi repository **random cá nhân của user**: created_by = currentUserId, class_id NULL, chapter_id NULL.
  - Có classId (+ chapterId) → giữ logic hiện tại: random theo part + class + chapter.

---

## 4. Luồng FE (gợi ý)

### 4.1 Chọn câu hỏi theo part

- **Đề cá nhân**:  
  - Gọi `GET /api/questions/by-part/{examPartId}` **không** truyền classId/chapterId.  
  - Hiển thị danh sách để user chọn, sau đó gọi `POST /api/tests/parts/questions` với `questionIds` đã chọn.
- **Đề lớp (có chapter)**:
  - Gọi `GET /api/questions/by-part/{examPartId}?classId=...&chapterId=...` (chapterId tùy chọn).  
  - Chọn câu → `POST /api/tests/parts/questions` với `questionIds`.

### 4.2 Random câu hỏi theo part

- **Đề cá nhân**:  
  - Gọi `POST /api/tests/parts/random-questions` với `{ testPartId, count }` (không gửi classId, chapterId).  
  - Backend sẽ random chỉ từ kho **cá nhân** của part đó.
- **Đề lớp (có chapter)**:
  - Gọi `POST /api/tests/parts/random-questions` với `{ testPartId, count, classId, chapterId? }`.  
  - Backend random từ kho **lớp/chapter** của part đó.

### 4.3 Tạo test gộp (FE nối API)

1. `POST /api/tests` (tạo test; cá nhân thì classId/chapterId = null).
2. `GET /api/exam-parts/by-exam-type/{examTypeId}` (lấy danh sách part, ví dụ TOEIC 7 part).
3. Với mỗi part:  
   - `POST /api/test-parts` (tạo test_part: testId, examPartId, numQuestions).  
   - Nếu “điền random”:  
     - Cá nhân: `POST /api/tests/parts/random-questions` với testPartId, count, không classId/chapterId.  
     - Lớp: cùng API nhưng có classId (và chapterId nếu có).

---

## 5. Tóm tắt thay đổi cần làm

| Vị trí | Nội dung |
|--------|----------|
| **QuestionRepository** | Thêm: list/count/random **cá nhân theo user** (created_by = userId, class_id/chapter_id NULL); list/count **lớp+chapter** (classId + chapterId nullable). |
| **QuestionService** | Sửa getQuestionsByPart & count: thêm chapterId và currentUserId (từ JWT); khi classId null → dùng repo cá nhân theo createdBy. |
| **QuestionController** | Thêm query `chapterId`; truyền HttpServletRequest để lấy userId; không classId = cá nhân của user đăng nhập. |
| **TestService.addRandomQuestionsToTestPart** | Nhận thêm currentUserId; khi không classId/chapterId → random **cá nhân của user** (created_by = currentUserId). |
| **FE** | Gửi JWT khi gọi by-part và random-questions; cá nhân = không gửi classId/chapterId; lớp = gửi classId (+ chapterId). |

Sau khi chỉnh backend theo hướng trên, luồng “chọn câu hỏi” và “random câu hỏi theo part” sẽ tách bạch **cá nhân** (chỉ kho của **tài khoản đang login**, không lấy hết) và **lớp học chứa chapter** (class + chapter).
