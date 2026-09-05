package com.example.spring.application.dto.response;

import com.example.spring.domain.model.ApprovalStatus;
import com.example.spring.domain.model.PurchaseApproval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponse {
    private Long id;
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private Long approverId;
    private String approverName;
    private String title;
    private String purpose;
    private String department;
    private BigDecimal totalAmount;
    private ApprovalStatus status;
    private String statusDescription;
    private String rejectionReason;
    private Long orderId;
    private LocalDateTime submittedDate;
    private LocalDateTime reviewedDate;
    private List<ApprovalItemResponse> items;

    public static ApprovalResponse from(PurchaseApproval approval) {
        if (approval == null) return null;

        List<ApprovalItemResponse> itemResponses = approval.getItems() != null
                ? approval.getItems().stream().map(ApprovalItemResponse::from).collect(Collectors.toList())
                : Collections.emptyList();

        return ApprovalResponse.builder()
                .id(approval.getId())
                .applicantId(approval.getApplicant().getId())
                .applicantName(approval.getApplicant().getName())
                .applicantEmail(approval.getApplicant().getEmail())
                .approverId(approval.getApprover() != null ? approval.getApprover().getId() : null)
                .approverName(approval.getApprover() != null ? approval.getApprover().getName() : null)
                .title(approval.getTitle())
                .purpose(approval.getPurpose())
                .department(approval.getDepartment())
                .totalAmount(approval.getTotalAmount())
                .status(approval.getStatus())
                .statusDescription(approval.getStatus().getDescription())
                .rejectionReason(approval.getRejectionReason())
                .orderId(approval.getOrderId())
                .submittedDate(approval.getSubmittedDate())
                .reviewedDate(approval.getReviewedDate())
                .items(itemResponses)
                .build();
    }
}