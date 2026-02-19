# CreateTestFromBankPage – Khớp BE & UI

## Thay đổi so với bản cũ

### 1. API khớp BE
- **Lấy câu theo part**: `GET /api/questions/by-exam-part/{id}` → `GET /api/questions/by-part/{examPartId}` (cá nhân: không gửi classId/chapterId; JWT gửi kèm header).
- **Gắn câu vào part**: `POST /api/test-parts/{id}/attach-from-bank` → `POST /api/tests/parts/questions` với body `{ testPartId, questionIds }`.
- **Random**: Không còn shuffle client-side. Khi chế độ Random, sau khi tạo test part gọi `POST /api/tests/parts/random-questions` với `{ testPartId, count }` (BE lấy câu random từ kho cá nhân theo JWT).

### 2. Luồng submit
1. `POST /api/tests` (tạo đề).
2. Với mỗi part có câu: `POST /api/test-parts` (tạo part) → lấy `testPartId` từ response.
3. Nếu part đang chế độ **Random**: `POST /api/tests/parts/random-questions` với `testPartId`, `count`.
4. Nếu part đang chế độ **Chọn thủ công**: `POST /api/tests/parts/questions` với `testPartId`, `questionIds`.

### 3. UI
- Subtitle rõ hơn: “Câu hỏi lấy theo kho cá nhân (tài khoản đang đăng nhập)”.
- Part badge: “Số câu: X” → “X câu”; tổng: “Tổng số câu sẽ đưa vào đề”.
- Danh sách câu dùng `<ul>`/`<li>`, thêm `role="list"`.
- Màu dùng tone indigo (#6366f1), bớt gradient, border/shadow nhẹ hơn.
- Hint part: “BE lấy ngẫu nhiên từ kho cá nhân”; khi không có câu: “Chưa có câu hỏi trong kho (cá nhân) cho part này.”.

### 4. Copy vào project
- Copy `CreateTestFromBankPage.jsx` và `CreateTestFromBankPage.module.scss` vào đúng thư mục trang (vd. `pages/` hoặc `features/`).
- Đổi import: `useBaseMetaData` và path `GlobalStyles` trong file SCSS cho đúng cấu trúc project.
- Đảm bảo axios gửi JWT (interceptor hoặc default header) khi gọi `/api/questions/by-part/...` và `/api/tests/...`.
