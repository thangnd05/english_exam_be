# SQL migrations

Chạy các file `.sql` theo thứ tự khi nâng cấp schema (MySQL, database `english_exam`).

- **migration_add_is_bank_to_questions.sql**: Thêm cột `is_bank` vào bảng `questions` để phân biệt câu trong kho và câu tạo gắn thẳng đề. Chạy một lần khi deploy code mới có logic `isBank`.

Cách chạy (từ thư mục project hoặc bất kỳ):

```bash
mysql -u your_user -p english_exam < docs/sql/migration_add_is_bank_to_questions.sql
```

Hoặc mở file trong MySQL Workbench / DBeaver và chạy nội dung.
