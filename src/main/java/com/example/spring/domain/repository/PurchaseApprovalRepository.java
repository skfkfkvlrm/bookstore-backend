package com.example.spring.domain.repository;

import com.example.spring.domain.model.ApprovalStatus;
import com.example.spring.domain.model.PurchaseApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseApprovalRepository extends JpaRepository<PurchaseApproval, Long> {
    Page<PurchaseApproval> findByApplicantIdOrderBySubmittedDateDesc(Long applicantId, Pageable pageable);
    List<PurchaseApproval> findByApplicantIdOrderBySubmittedDateDesc(Long applicantId);
    Page<PurchaseApproval> findByStatusOrderBySubmittedDateDesc(ApprovalStatus status, Pageable pageable);
    Page<PurchaseApproval> findAllByOrderBySubmittedDateDesc(Pageable pageable);
    long countByStatus(ApprovalStatus status);
}