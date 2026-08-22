-- auth-service: V2__seed_users.sql
-- Dữ liệu người dùng mẫu chuẩn hóa cho Sportify (20 tài khoản)

INSERT INTO users (id, username, email, phone, full_name, keycloak_id, status, created_at) VALUES
(1,  'admin',          'admin@sportify.vn',     '0901000001', 'Nguyễn Quản Trị',  'kc-user-uuid-0001-admin',              'ACTIVE', '2026-05-01 08:00:00'),
(2,  'nguyenvan_a',    'nguyenvana@gmail.com',  '0912345001', 'Nguyễn Văn An',    'kc-user-uuid-0002-nguyen-van-an',       'ACTIVE', '2026-05-02 09:15:00'),
(3,  'tranminh_b',     'tranminhb@gmail.com',   '0912345002', 'Trần Minh Bảo',    'kc-user-uuid-0003-tran-minh-bao',      'ACTIVE', '2026-05-03 10:20:00'),
(4,  'lehong_c',      'lehongc@gmail.com',    '0912345003', 'Lê Hồng Chi',      'kc-user-uuid-0004-le-hong-chi',        'ACTIVE', '2026-05-04 11:00:00'),
(5,  'phamthi_d',      'phamthid@gmail.com',   '0912345004', 'Phạm Thị Dung',    'kc-user-uuid-0005-pham-thi-dung',      'ACTIVE', '2026-05-05 14:30:00'),
(6,  'hoangduc_e',     'hoangduce@gmail.com',  '0912345005', 'Hoàng Đức Em',     'kc-user-uuid-0006-hoang-duc-em',       'ACTIVE', '2026-05-06 15:45:00'),
(7,  'vuthianh_f',     'vuthianhf@gmail.com',  '0912345006', 'Vũ Thị Anh',       'kc-user-uuid-0007-vu-thi-anh',         'ACTIVE', '2026-05-07 16:10:00'),
(8,  'dangquoc_g',     'dangquocg@gmail.com',  '0912345007', 'Đặng Quốc Gia',    'kc-user-uuid-0008-dang-quoc-gia',      'ACTIVE', '2026-05-08 17:25:00'),
(9,  'buihai_h',       'buihaih@gmail.com',    '0912345008', 'Bùi Hải Hà',       'kc-user-uuid-0009-bui-hai-ha',         'ACTIVE', '2026-05-09 18:00:00'),
(10, 'ngothien_i',     'ngothieni@gmail.com',  '0912345009', 'Ngô Thiện Ích',    'kc-user-uuid-0010-ngo-thien-ich',      'ACTIVE', '2026-05-10 19:30:00'),
(11, 'dothikhanh_k',   'dothikhanhk@gmail.com', '0912345010', 'Đỗ Thị Khánh',     'kc-user-uuid-0011-do-thi-khanh',       'ACTIVE', '2026-05-11 08:20:00'),
(12, 'lytrung_l',      'lytrungl@gmail.com',   '0912345011', 'Lý Trung Linh',    'kc-user-uuid-0012-ly-trung-linh',      'ACTIVE', '2026-05-12 09:40:00'),
(13, 'maiquang_m',     'maiquangm@gmail.com',  '0912345012', 'Mai Quang Minh',   'kc-user-uuid-0013-mai-quang-minh',     'ACTIVE', '2026-05-13 10:50:00'),
(14, 'nguyenthi_n',    'nguyenthin@gmail.com', '0912345013', 'Nguyễn Thị Ngọc',  'kc-user-uuid-0014-nguyen-thi-ngoc',    'ACTIVE', '2026-05-14 11:30:00'),
(15, 'phanhuu_o',      'phanhuuo@gmail.com',   '0912345014', 'Phan Hữu Oanh',    'kc-user-uuid-0015-phan-huu-oanh',      'ACTIVE', '2026-05-15 13:15:00'),
(16, 'quachvan_p',     'quachvanp@gmail.com',  '0912345015', 'Quách Văn Phong',  'kc-user-uuid-0016-quach-van-phong',    'ACTIVE', '2026-05-16 14:00:00'),
(17, 'trieuhoang_q',   'trieuhoangq@gmail.com','0912345016', 'Triệu Hoàng Quân', 'kc-user-uuid-0017-trieu-hoang-quan',   'ACTIVE', '2026-05-17 15:20:00'),
(18, 'songuyen_r',     'songuyenr@gmail.com',  '0912345018', 'Sơn Nguyên Rin',   'kc-user-uuid-0018-son-nguyen-rin',     'ACTIVE', '2026-05-18 16:45:00'),
(19, 'tranthien_s',    'tranthiens@gmail.com', '0912345019', 'Trần Thiện Sơn',   'kc-user-uuid-0019-tran-thien-son',     'ACTIVE', '2026-05-19 17:50:00'),
(20, 'vuonghai_t',     'vuonghait@gmail.com',  '0912345020', 'Vương Hải Tùng',   'kc-user-uuid-0020-vuong-hai-tung',     'ACTIVE', '2026-05-20 18:30:00');

