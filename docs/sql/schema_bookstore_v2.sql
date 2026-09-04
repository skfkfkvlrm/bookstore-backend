-- ========================================================
-- DayBySpring Bookstore v2 - Complete Database Schema DDL
-- Target DBMS: MySQL 8.0+ / MariaDB 10.5+ / ERDCloud Import
-- Generated Based on: JPA Entities & Rich Domain Models
-- Entities: Member, Book, Loan, Order, OrderItem, Payment, Delivery, Refund
-- ========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS refunds;
DROP TABLE IF EXISTS deliveries;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS member;

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================
-- 1. 회원 (member) 테이블
-- ========================================================
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 고유 식별자 PK',
    name VARCHAR(100) NOT NULL COMMENT '회원 성명',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '로그인 이메일 (유니크)',
    password VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '회원 역할 (USER, ADMIN)',
    membership_type VARCHAR(20) DEFAULT 'REGULAR' COMMENT '멤버십 등급 (REGULAR, PREMIUM)',
    join_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 기본 정보 테이블';

CREATE INDEX idx_member_email ON member (email);
CREATE INDEX idx_member_name ON member (name);
CREATE INDEX idx_member_role ON member (role);

-- ========================================================
-- 2. 도서 (book) 테이블
-- ========================================================
CREATE TABLE book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '도서 고유 식별자 PK',
    title VARCHAR(200) NOT NULL COMMENT '도서 제목',
    author VARCHAR(100) NOT NULL COMMENT '저자명',
    isbn VARCHAR(17) NOT NULL UNIQUE COMMENT '국제표준도서번호 ISBN (유니크)',
    price DECIMAL(10, 2) NOT NULL COMMENT '도서 정가 (Money VO)',
    price_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '통화 코드',
    available BOOLEAN NOT NULL DEFAULT TRUE COMMENT '대여/구매 가능 여부',
    cover_image_url VARCHAR(500) NULL COMMENT '도서 표지 이미지 URL',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '도서 등록 일시',
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '정보 수정 일시',
    deleted_date DATETIME NULL COMMENT '소프트 삭제(Soft Delete) 일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도서 카탈로그 정보 테이블';

CREATE INDEX idx_book_isbn ON book (isbn);
CREATE INDEX idx_book_title ON book (title);
CREATE INDEX idx_book_author ON book (author);

-- ========================================================
-- 3. 도서 대여 (loan) 테이블
-- ========================================================
CREATE TABLE loan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '대여 고유 식별자 PK',
    member_id BIGINT NOT NULL COMMENT '대여 회원 FK',
    book_id BIGINT NOT NULL COMMENT '대여 도서 FK',
    loan_date DATETIME NOT NULL COMMENT '대여 시작 일시',
    due_date DATETIME NOT NULL COMMENT '반납 예정 일시',
    return_date DATETIME NULL COMMENT '실제 반납 일시',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '대여 상태 (ACTIVE, RETURNED, OVERDUE, CANCELLED)',
    overdue_fee DECIMAL(10, 2) DEFAULT 0.00 COMMENT '누적 연체료',
    overdue_fee_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '연체료 통화 코드',
    extension_count INT NOT NULL DEFAULT 0 COMMENT '반납 연장 횟수',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT fk_loan_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE RESTRICT,
    CONSTRAINT fk_loan_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도서 대여 내역 테이블';

CREATE INDEX idx_loan_member_id ON loan (member_id);
CREATE INDEX idx_loan_book_id ON loan (book_id);
CREATE INDEX idx_loan_date ON loan (loan_date);
CREATE INDEX idx_loan_status ON loan (status);

-- ========================================================
-- 4. 주문 (orders) 테이블
-- ========================================================
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 고유 식별자 PK',
    member_id BIGINT NOT NULL COMMENT '주문 고객 FK',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '총 주문 금액',
    total_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '통화 단위',
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '할인 적용 금액',
    discount_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '할인 통화 단위',
    points_used INT NOT NULL DEFAULT 0 COMMENT '사용 포인트',
    points_earned INT NOT NULL DEFAULT 0 COMMENT '적립 예정 포인트',
    coupon_code VARCHAR(50) NULL COMMENT '적용 쿠폰 코드',
    order_date DATETIME NOT NULL COMMENT '주문 접수 일시',
    confirmed_date DATETIME NULL COMMENT '주문 확정 일시',
    shipped_date DATETIME NULL COMMENT '출고 일시',
    delivered_date DATETIME NULL COMMENT '배송 완료 일시',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '주문 상태 (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)',
    cancelled_date DATETIME NULL COMMENT '주문 취소 일시',
    cancellation_reason VARCHAR(255) NULL COMMENT '취소 사유',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도서 구매 주문 마스터 테이블';

CREATE INDEX idx_order_member_id ON orders (member_id);
CREATE INDEX idx_order_date ON orders (order_date);
CREATE INDEX idx_order_status ON orders (status);
CREATE INDEX idx_order_created_date ON orders (created_date);

-- ========================================================
-- 5. 주문 상세 품목 (order_item) 테이블
-- ========================================================
CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 상세 품목 식별자 PK',
    order_id BIGINT NOT NULL COMMENT '소속 주문 FK',
    book_id BIGINT NOT NULL COMMENT '주문 도서 FK',
    quantity INT NOT NULL DEFAULT 1 COMMENT '구매 수량',
    price DECIMAL(10, 2) NOT NULL COMMENT '주문 당시 도서 단가',
    price_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '단가 통화 단위',
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 상세 품목 매핑 테이블';

CREATE INDEX idx_order_item_order_id ON order_item (order_id);
CREATE INDEX idx_order_item_book_id ON order_item (book_id);

-- ========================================================
-- 6. 결제 (payments) 테이블
-- ========================================================
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 고유 식별자 PK',
    order_id BIGINT NOT NULL UNIQUE COMMENT '대상 주문 FK (1:1 관계)',
    method VARCHAR(30) NOT NULL COMMENT '결제 수단 (CREDIT_CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, EASY_PAY)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '결제 상태 (PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED, PARTIAL_REFUNDED)',
    amount DECIMAL(10, 2) NOT NULL COMMENT '실제 결제 승인 금액',
    amount_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '결제 통화',
    payment_date DATETIME NULL COMMENT '결제 승인 완료 일시',
    transaction_id VARCHAR(100) NULL UNIQUE COMMENT 'PG사 거래 고유 트랜잭션 ID',
    pg_provider VARCHAR(50) NULL COMMENT '연동 PG사 (TOSS, NICE, INICIS)',
    card_company VARCHAR(50) NULL COMMENT '카드사명',
    card_number VARCHAR(30) NULL COMMENT '마스킹된 카드번호',
    installment_months INT NULL DEFAULT 0 COMMENT '할부 개월 수 (0: 일시불)',
    failure_reason VARCHAR(255) NULL COMMENT '결제 실패 사유',
    failed_date DATETIME NULL COMMENT '결제 실패 일시',
    cancelled_date DATETIME NULL COMMENT '결제 취소 일시',
    refunded_amount DECIMAL(10, 2) NULL DEFAULT 0.00 COMMENT '환불된 누적 금액',
    refunded_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '환불 통화 단위',
    refunded_date DATETIME NULL COMMENT '최종 환불 일시',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 결제 거래 내역 테이블';

CREATE INDEX idx_payment_order_id ON payments (order_id);
CREATE INDEX idx_payment_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payment_status ON payments (status);

-- ========================================================
-- 7. 배송 (deliveries) 테이블
-- ========================================================
CREATE TABLE deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '배송 고유 식별자 PK',
    order_id BIGINT NOT NULL UNIQUE COMMENT '대상 주문 FK (1:1 관계)',
    recipient_name VARCHAR(100) NOT NULL COMMENT '수령인 성명',
    phone_number VARCHAR(30) NOT NULL COMMENT '수령인 연락처',
    zip_code VARCHAR(10) NULL COMMENT '우편번호',
    address VARCHAR(255) NOT NULL COMMENT '기본 배송 주소',
    address_detail VARCHAR(255) NULL COMMENT '상세 주소',
    delivery_memo VARCHAR(500) NULL COMMENT '배송 기사 전달 메모',
    status VARCHAR(30) NOT NULL DEFAULT 'PREPARING' COMMENT '배송 상태 (PREPARING, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED)',
    tracking_number VARCHAR(100) NULL COMMENT '운송장 번호',
    courier_company VARCHAR(50) NULL COMMENT '택배 배송사명',
    shipped_date DATETIME NULL COMMENT '출고 일시',
    delivered_date DATETIME NULL COMMENT '배송 완료 일시',
    estimated_delivery_date DATETIME NULL COMMENT '배송 도착 예정 일시',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 배송 정보 테이블';

CREATE INDEX idx_delivery_order_id ON deliveries (order_id);
CREATE INDEX idx_delivery_status ON deliveries (status);

-- ========================================================
-- 8. 환불 (refunds) 테이블
-- ========================================================
CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '환불 고유 식별자 PK',
    order_id BIGINT NOT NULL COMMENT '대상 주문 FK (부분 환불 지원으로 N:1 매핑)',
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' COMMENT '환불 상태 (REQUESTED, APPROVED, PROCESSING, COMPLETED, REJECTED, FAILED)',
    amount DECIMAL(10, 2) NOT NULL COMMENT '환불 요청 금액',
    amount_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '환불 통화 단위',
    reason VARCHAR(1000) NOT NULL COMMENT '환불 사유',
    requested_by VARCHAR(100) NULL COMMENT '환불 요청자 식별자',
    request_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '환불 접수 일시',
    approved_date DATETIME NULL COMMENT '환불 승인 일시',
    rejected_date DATETIME NULL COMMENT '환불 거부 일시',
    completed_date DATETIME NULL COMMENT '환불 정산 완료 일시',
    approved_by VARCHAR(100) NULL COMMENT '승인 관리자',
    rejected_by VARCHAR(100) NULL COMMENT '거부 관리자',
    rejection_reason VARCHAR(500) NULL COMMENT '거부 사유',
    bank_name VARCHAR(50) NULL COMMENT '환불 대상 은행명',
    account_number VARCHAR(50) NULL COMMENT '환불 계좌번호',
    account_holder VARCHAR(50) NULL COMMENT '예금주명',
    refund_transaction_id VARCHAR(100) NULL COMMENT 'PG/은행 환불 거래 번호',
    processing_memo VARCHAR(500) NULL COMMENT '정산 처리 메모',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주문 취소 및 환불 관리 테이블';

CREATE INDEX idx_refund_order_id ON refunds (order_id);
CREATE INDEX idx_refund_status ON refunds (status);
