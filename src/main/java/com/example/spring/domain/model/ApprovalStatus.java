package com.example.spring.domain.model;

/**
 * 도서 구매 품의(결재) 상태
 */
public enum ApprovalStatus {
    PENDING("결재 대기"),
    APPROVED("승인 완료"),
    REJECTED("반려"),
    ORDERED("주문 생성 완료"),
    CANCELLED("상신 취소");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}