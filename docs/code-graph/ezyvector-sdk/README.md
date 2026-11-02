# Đồ thị mã ezyvector-sdk

Mở `viewer.html` trong trình duyệt (file tự chứa, không cần mạng).

- **Phạm vi:** 55 file trong `ezyvector-sdk/src/main/java`. Method được tách riêng cho 3 file lõi: `EzyVectorService`, `EzyVectorFileStorage`, `HnswIndex`.
- **Số liệu:** 134 node, 412 cạnh. Cạnh lấy từ AST (graphify); không có cạnh nào do người viết thêm.
- **Lane:** 0 Dùng chung · 1 Service · 2 Converter · 3 Repository · 4 Pagination · 5 Dữ liệu domain · 6 Engine vector.
- **Luật phụ thuộc:** lane số nhỏ chỉ được dùng lane số lớn hơn; lane 0 được miễn. Hiện có 0 vi phạm.
- **Curate:** đối chiếu với commit `27e4def`. Mỗi node trong `curation.json` lưu thêm `curated_sha` (sha256 rút gọn của file lúc curate).
- **Cờ lỗi:** 8 `bug`, 6 `duplicate`, 1 `dead`. Lý do của từng cờ nằm trong `gotchas` của node đó.

## Giới hạn đã biết

graphify không trích xuất `import static` và truy cập hằng enum. Vì vậy `EzyVectorConstants`, `EzyVectorTableNames` và các `*PaginationSortOrder` hiển thị "dùng bởi 0" dù thực tế có người dùng; ghi chú nằm trong `gotchas` của từng node.

## Cập nhật khi code thay đổi

1. So `curated_sha` với sha hiện tại của từng file. File nào đổi sha thì đọc lại và sửa curation **bằng tay**.
2. Chỉ khi cấu trúc đổi (thêm/xóa/đổi tên file, method), trích xuất lại:

   ```bash
   pip install graphifyy
   python3 scripts/extract_run.py <repo>/ezyvector-sdk/src/main/java <thư-mục-cache-ngoài-repo> ast-raw.json
   python3 scripts/arrange.py ast-raw.json data.json
   ```

3. Ghép lại viewer:

   ```bash
   python3 scripts/assemble.py scripts/viewer-template.vi.html data.json curation.json meta.json <repo>/ezyvector-sdk/src/main/java/org/youngmonkeys/ezyvector viewer.html
   ```

   Lệnh này in ra số node chưa curate, số curation mồ côi và số node trỏ tới file không còn tồn tại. Cả ba phải bằng 0.

`merge_curation.py` chỉ **thêm** trường còn trống và không bao giờ ghi đè nội dung đã viết. Không có script nào sinh nội dung `what`/`why`.
