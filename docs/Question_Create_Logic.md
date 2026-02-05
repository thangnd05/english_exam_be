# Logic tạo câu hỏi – Thiết kế theo cơ chế

## Quy tắc

| Cơ chế | Có nên làm | Cách làm |
|--------|------------|----------|
| **Tạo → Kho câu hỏi** | ✅ BẮT BUỘC | Mọi câu hỏi tạo ra **luôn** lưu vào bảng `questions` (kho). Không có luồng "tạo câu chỉ để gắn đề" mà không lưu kho. |
| **Tạo → Gắn passage** | ✅ BẮT BUỘC | Khi tạo câu hỏi **phải** xử lý passage: có passage trong request → tạo/lưu `Passage` trước, gán `question.passageId`; không có (câu đơn) → `passageId = null`. |
| **Tạo → Gắn thẳng đề** | ⚠️ Hạn chế | **Không** có API "tạo câu hỏi mới và gắn luôn vào test" làm luồng chính. Gắn đề qua bước riêng: chọn câu từ kho → gọi API "gắn câu vào part" (`AddQuestionsToTestRequest`). |

## Luồng chuẩn

1. **Tạo câu hỏi vào kho**
   - **1 câu:** `POST /api/questions` body `QuestionCreateRequest` (examPartId, passage?, questionText, questionType, answers, classId?, chapterId?).
   - **Nhiều câu cùng passage:** `POST /api/questions/bulk-with-passage` body `BulkQuestionWithPassageRequest` + optional file audio (LISTENING).
   - Luôn: tạo Passage (nếu có) → lưu Question → lưu Answers. **Không** nhận `testPartId` trong request (không gắn thẳng đề).

2. **Gắn câu hỏi từ kho vào đề**
   - `POST /api/tests/parts/{testPartId}/questions` body `AddQuestionsToTestRequest` (hoặc `questionIds`).
   - Chỉ tạo bản ghi `test_questions` (testPartId, questionId). Câu hỏi đã phải tồn tại trong kho.

## "Gắn thẳng đề" (hạn chế)

- Nếu vẫn cần bước "sau khi tạo câu, gắn luôn vào 1 part": có thể thêm **optional** `testPartId` trong request tạo câu. Trong service: **trước** phải lưu câu vào kho, **sau** nếu `testPartId != null` mới gắn vào part. Document rõ là tính năng phụ, hạn chế dùng; ưu tiên luồng "tạo vào kho → gắn đề bằng API riêng".

---

## API đã implement

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/questions` | Tạo 1 câu vào kho. Body: `QuestionCreateRequest` (examPartId, passage?, questionText, questionType, answers, classId?, chapterId?). Luôn lưu kho; xử lý passage nếu có. |
| POST | `/api/questions/bulk-with-passage` | Tạo nhiều câu cùng 1 passage vào kho. Multipart: `request` (JSON string của `BulkQuestionWithPassageRequest`), `audio` (optional, cho LISTENING). Passage bắt buộc. |
| POST | `/api/tests/parts/questions` | Gắn câu hỏi từ kho vào part của đề. Body: `AddQuestionsToTestRequest` (testPartId, questionIds). Chỉ tạo `test_questions`; không tạo câu hỏi mới. |
