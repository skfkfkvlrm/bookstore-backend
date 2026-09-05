package com.example.spring.application;

import com.example.spring.application.dto.request.CreateApprovalRequest;
import com.example.spring.application.dto.response.ApprovalResponse;
import com.example.spring.domain.model.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApprovalService {
    ApprovalResponse createApproval(Long memberId, CreateApprovalRequest request);
    Page<ApprovalResponse> getMyApprovals(Long memberId, Pageable pageable);
    Page<ApprovalResponse> getAllApprovals(ApprovalStatus status, Pageable pageable);
    ApprovalResponse getApprovalById(Long id);
    ApprovalResponse approve(Long approvalId, Long approverId);
    ApprovalResponse reject(Long approvalId, Long approverId, String reason);
}