package com.example.spring.application.service;

import com.example.spring.application.ApprovalService;
import com.example.spring.application.dto.request.CreateApprovalRequest;
import com.example.spring.application.dto.response.ApprovalResponse;
import com.example.spring.domain.model.*;
import com.example.spring.domain.repository.BookRepository;
import com.example.spring.domain.repository.MemberRepository;
import com.example.spring.domain.repository.OrderRepository;
import com.example.spring.domain.repository.PurchaseApprovalRepository;
import com.example.spring.domain.vo.Address;
import com.example.spring.domain.vo.Money;
import com.example.spring.exception.BusinessException;
import com.example.spring.exception.MemberException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final PurchaseApprovalRepository approvalRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ApprovalResponse createApproval(Long memberId, CreateApprovalRequest request) {
        log.info("도서 구매 품의 상신 - 회원 ID: {}, 제목: {}", memberId, request.getTitle());

        Member applicant = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException.MemberNotFoundException(memberId));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseApprovalItem> approvalItems = new ArrayList<>();

        for (CreateApprovalRequest.ApprovalItemDto itemDto : request.getItems()) {
            BigDecimal price = itemDto.getEstimatedPrice() != null ? itemDto.getEstimatedPrice() : BigDecimal.valueOf(15000);
            int qty = itemDto.getQuantity() != null && itemDto.getQuantity() > 0 ? itemDto.getQuantity() : 1;
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(itemTotal);

            PurchaseApprovalItem item = PurchaseApprovalItem.builder()
                    .bookId(itemDto.getBookId())
                    .bookTitle(itemDto.getBookTitle())
                    .bookAuthor(itemDto.getBookAuthor())
                    .isbn(itemDto.getIsbn())
                    .quantity(qty)
                    .estimatedPrice(price)
                    .totalPrice(itemTotal)
                    .build();

            approvalItems.add(item);
        }

        PurchaseApproval approval = PurchaseApproval.builder()
                .applicant(applicant)
                .title(request.getTitle())
                .purpose(request.getPurpose())
                .department(request.getDepartment())
                .totalAmount(totalAmount)
                .status(ApprovalStatus.PENDING)
                .submittedDate(LocalDateTime.now())
                .build();

        for (PurchaseApprovalItem item : approvalItems) {
            approval.addItem(item);
        }

        PurchaseApproval saved = approvalRepository.save(approval);
        log.info("도서 구매 품의 상신 완료 - 품의 ID: {}, 총 예산: {}", saved.getId(), saved.getTotalAmount());
        return ApprovalResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalResponse> getMyApprovals(Long memberId, Pageable pageable) {
        return approvalRepository.findByApplicantIdOrderBySubmittedDateDesc(memberId, pageable)
                .map(ApprovalResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalResponse> getAllApprovals(ApprovalStatus status, Pageable pageable) {
        if (status != null) {
            return approvalRepository.findByStatusOrderBySubmittedDateDesc(status, pageable)
                    .map(ApprovalResponse::from);
        }
        return approvalRepository.findAllByOrderBySubmittedDateDesc(pageable)
                .map(ApprovalResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalResponse getApprovalById(Long id) {
        PurchaseApproval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("APPROVAL_NOT_FOUND", "품의 문서를 찾을 수 없습니다: id=" + id));
        return ApprovalResponse.from(approval);
    }

    @Override
    @Transactional
    public ApprovalResponse approve(Long approvalId, Long approverId) {
        log.info("도서 구매 품의 승인 요청 - 품의 ID: {}, 결재자 ID: {}", approvalId, approverId);

        PurchaseApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException("APPROVAL_NOT_FOUND", "품의 문서를 찾을 수 없습니다: id=" + approvalId));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("INVALID_APPROVAL_STATE", "대기 중인 품의만 승인할 수 있습니다. 현재 상태: " + approval.getStatus());
        }

        Member approver = memberRepository.findById(approverId)
                .orElseThrow(() -> new MemberException.MemberNotFoundException(approverId));

        // 승인 시 자동으로 대량 구매 주문서(Order) 생성 및 연계
        Long createdOrderId = null;
        try {
            Order order = Order.builder()
                    .member(approval.getApplicant())
                    .totalAmount(Money.of(approval.getTotalAmount()))
                    .discountAmount(Money.zero())
                    .orderDate(LocalDateTime.now())
                    .status(OrderStatus.CONFIRMED)
                    .confirmedDate(LocalDateTime.now())
                    .build();

            for (PurchaseApprovalItem appItem : approval.getItems()) {
                Book book = null;
                if (appItem.getBookId() != null) {
                    book = bookRepository.findById(appItem.getBookId()).orElse(null);
                }
                if (book == null) {
                    book = bookRepository.findAll().stream().findFirst().orElse(null);
                }

                if (book != null) {
                    OrderItem orderItem = OrderItem.builder()
                            .book(book)
                            .quantity(appItem.getQuantity())
                            .price(Money.of(appItem.getEstimatedPrice()))
                            .build();
                    order.addOrderItem(orderItem);
                }
            }

            // 기관 결재 승인 결제 정보 생성
            Payment payment = Payment.builder()
                    .order(order)
                    .method(PaymentMethod.BANK_TRANSFER)
                    .status(PaymentStatus.COMPLETED)
                    .amount(Money.of(approval.getTotalAmount()))
                    .pgProvider("INSTITUTION_APPROVAL")
                    .transactionId("APPR_ORDER_" + approval.getId())
                    .paymentDate(LocalDateTime.now())
                    .build();

            Delivery delivery = Delivery.builder()
                    .order(order)
                    .recipientName(approval.getApplicant().getName())
                    .phoneNumber("010-0000-0000")
                    .deliveryAddress(Address.of("06234", "도서관 행정지원실", approval.getDepartment() != null ? approval.getDepartment() : "품의 도서 수령처"))
                    .deliveryMemo("전자결재 승인 건 (" + approval.getTitle() + ")")
                    .build();

            order.attachPayment(payment);
            order.attachDelivery(delivery);

            Order savedOrder = orderRepository.save(order);
            createdOrderId = savedOrder.getId();
            log.info("전자결재 승인 연계 주문 자동 생성 완료 - 주문 ID: {}", createdOrderId);
        } catch (Exception e) {
            log.warn("주문 자동 생성 실패 (품의 상태만 승인 처리): {}", e.getMessage());
        }

        approval.approve(approver, createdOrderId);
        PurchaseApproval saved = approvalRepository.save(approval);
        return ApprovalResponse.from(saved);
    }

    @Override
    @Transactional
    public ApprovalResponse reject(Long approvalId, Long approverId, String reason) {
        log.info("도서 구매 품의 반려 요청 - 품의 ID: {}, 결재자 ID: {}, 사유: {}", approvalId, approverId, reason);

        PurchaseApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException("APPROVAL_NOT_FOUND", "품의 문서를 찾을 수 없습니다: id=" + approvalId));

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("INVALID_APPROVAL_STATE", "대기 중인 품의만 반려할 수 있습니다. 현재 상태: " + approval.getStatus());
        }

        Member approver = memberRepository.findById(approverId)
                .orElseThrow(() -> new MemberException.MemberNotFoundException(approverId));

        approval.reject(approver, reason);
        PurchaseApproval saved = approvalRepository.save(approval);
        return ApprovalResponse.from(saved);
    }
}