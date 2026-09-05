package com.example.spring.domain.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_approvals", indexes = {
        @Index(name = "idx_approval_applicant", columnList = "applicant_id"),
        @Index(name = "idx_approval_status", columnList = "status"),
        @Index(name = "idx_approval_submitted_date", columnList = "submitted_date")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기안자 (신청인)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Member applicant;

    // 최종 결재권자 (관리자/사서)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Member approver;

    // 품의 제목
    @Column(nullable = false, length = 200)
    private String title;

    // 품의 목적 및 사유
    @Column(columnDefinition = "TEXT")
    private String purpose;

    // 신청 부서/학과/소속
    @Column(length = 100)
    private String department;

    // 품의 신청 도서 항목들
    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonManagedReference
    private List<PurchaseApprovalItem> items = new ArrayList<>();

    // 총 추정 소요 예산
    @Column(nullable = false)
    private BigDecimal totalAmount;

    // 결재 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    // 반려 사유 (반려 시 필수)
    @Column(length = 500)
    private String rejectionReason;

    // 승인 후 자동 생성된 주문 ID (1:1 연계)
    @Column(name = "order_id")
    private Long orderId;

    // 일시
    @Column(name = "submitted_date", nullable = false)
    private LocalDateTime submittedDate;

    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;

    @PrePersist
    protected void onCreate() {
        if (submittedDate == null) {
            submittedDate = LocalDateTime.now();
        }
    }

    public void addItem(PurchaseApprovalItem item) {
        this.items.add(item);
        item.attachToApproval(this);
    }

    public void approve(Member approver, Long createdOrderId) {
        this.status = ApprovalStatus.APPROVED;
        this.approver = approver;
        this.reviewedDate = LocalDateTime.now();
        if (createdOrderId != null) {
            this.orderId = createdOrderId;
        }
    }

    public void reject(Member approver, String reason) {
        this.status = ApprovalStatus.REJECTED;
        this.approver = approver;
        this.rejectionReason = reason;
        this.reviewedDate = LocalDateTime.now();
    }
}