package com.example.spring.presentation.controller;

import com.example.spring.application.ApprovalService;
import com.example.spring.application.dto.request.CreateApprovalRequest;
import com.example.spring.application.dto.request.RejectApprovalRequest;
import com.example.spring.application.dto.response.ApprovalResponse;
import com.example.spring.domain.model.ApprovalStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Approval", description = "도서 구매 승인/품의 전자결재 API")
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 도서 구매 품의 상신 (신청)
     */
    @Operation(summary = "도서 구매 품의 상신")
    @PostMapping
    public ResponseEntity<ApprovalResponse> createApproval(
            @RequestParam(required = false) Long memberId,
            @Valid @RequestBody CreateApprovalRequest request) {

        Long targetMemberId = memberId != null ? memberId : request.getMemberId();
        if (targetMemberId == null) {
            targetMemberId = 2L; // 기본 일반회원 ID
        }

        log.info("도서 구매 품의 상신 요청 - 회원 ID: {}, 제목: {}", targetMemberId, request.getTitle());
        ApprovalResponse response = approvalService.createApproval(targetMemberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 품의 내역 조회
     */
    @Operation(summary = "내 품의 내역 조회")
    @GetMapping("/my")
    public ResponseEntity<Page<ApprovalResponse>> getMyApprovals(
            @RequestParam(defaultValue = "2") Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ApprovalResponse> responses = approvalService.getMyApprovals(memberId, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * 전체 결재함 조회 (관리자용)
     */
    @Operation(summary = "전체 결재 목록 조회 (관리자용)")
    @GetMapping
    public ResponseEntity<Page<ApprovalResponse>> getAllApprovals(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ApprovalResponse> responses = approvalService.getAllApprovals(status, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * 품의 상세 조회
     */
    @Operation(summary = "품의 문서 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalResponse> getApprovalById(@PathVariable Long id) {
        ApprovalResponse response = approvalService.getApprovalById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 품의 승인 처리 (관리자)
     */
    @Operation(summary = "품의 승인 처리")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApprovalResponse> approve(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long approverId) {

        log.info("품의 승인 처리 - ID: {}, 결재자: {}", id, approverId);
        ApprovalResponse response = approvalService.approve(id, approverId);
        return ResponseEntity.ok(response);
    }

    /**
     * 품의 반려 처리 (관리자)
     */
    @Operation(summary = "품의 반려 처리")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApprovalResponse> reject(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long approverId,
            @Valid @RequestBody RejectApprovalRequest request) {

        log.info("품의 반려 처리 - ID: {}, 결재자: {}, 사유: {}", id, approverId, request.getReason());
        ApprovalResponse response = approvalService.reject(id, approverId, request.getReason());
        return ResponseEntity.ok(response);
    }
}