-- Migration: Thêm cột is_bank vào bảng questions
-- Khớp với model Question (isBank: true = câu trong kho, false = câu tạo gắn thẳng đề)
-- Chạy trên database english_exam (MySQL 8.x)

USE english_exam;

-- Thêm cột (nullable, mặc định 1 = trong kho cho dữ liệu cũ)
ALTER TABLE questions
ADD COLUMN is_bank TINYINT(1) DEFAULT 1
COMMENT '1=trong kho, 0=tạo gắn thẳng đề (không lưu kho)'
AFTER chapter_id;

-- Cập nhật các bản ghi cũ: coi tất cả là câu trong kho (nếu cột vừa thêm có DEFAULT 1 thì bước này không bắt buộc)
-- UPDATE questions SET is_bank = 1 WHERE is_bank IS NULL;
