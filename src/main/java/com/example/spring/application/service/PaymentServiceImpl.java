package com.example.spring.application.service;

import com.example.spring.application.PaymentService;
import com.example.spring.application.dto.request.PaymentConfirmRequest;
import com.example.spring.application.dto.response.PaymentResponse;
import com.example.spring.domain.event.OrderConfirmedEvent;
import com.example.spring.domain.event.PaymentCompletedEvent;
import com.example.spring.domain.event.PaymentFailedEvent;
import com.example.spring.domain.model.Order;
import com.example.spring.domain.model.OrderStatus;
import com.example.spring.domain.model.Payment;
import com.example.spring.domain.model.PaymentStatus;
import com.example.spring.domain.repository.OrderRepository;
import com.example.spring.domain.repository.PaymentRepository;
import com.example.spring.domain.vo.Money;
import com.example.spring.exception.OrderException;
import com.example.spring.exception.PaymentException;
import com.example.spring.infrastructure.payment.PaymentApproveResult;
import com.example.spring.infrastructure.payment.PaymentGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException("결제 정보를 찾을 수 없습니다: orderId=" + orderId));
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(id));
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> findByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
        log.info("2단계 결제 승인 처리 시작 - 주문 ID: {}, 결제 키: {}, 금액: {}",
                request.getOrderId(), request.getPaymentKey(), request.getAmount());

        // 1. 주문 조회 및 상태 검증
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderException.OrderNotFoundException(request.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new PaymentException.InvalidPaymentStateException("대기 중(PENDING) 상태인 주문만 결제할 수 있습니다. 현재 상태: " + order.getStatus());
        }

        // 2. 결제 엔티티 조회
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = paymentRepository.findByOrderId(order.getId())
                    .orElseThrow(() -> new PaymentException.PaymentNotFoundException("주문에 연결된 결제 정보가 없습니다: orderId=" + order.getId()));
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException.InvalidPaymentStateException("이미 처리된 결제입니다. 현재 상태: " + payment.getStatus());
        }

        // 3. 결제 금액 위변조 검증 (주문 총액과 PG 승인 요청액 대조)
        BigDecimal expectedAmount = order.getFinalAmount().getAmount();
        if (expectedAmount.compareTo(request.getAmount()) != 0) {
            log.error("결제 금액 위변조 감지! 주문 금액: {}, 요청 금액: {}", expectedAmount, request.getAmount());
            payment.fail("결제 금액 불일치 위변조 시도 감지");
            paymentRepository.save(payment);
            throw new PaymentException.InvalidPaymentAmountException(
                    "결제 금액이 일치하지 않습니다. 주문 금액: " + expectedAmount + ", 요청 결제 금액: " + request.getAmount());
        }

        // 4. PG사 2단계 승인 API 호출
        PaymentApproveResult approveResult = paymentGatewayClient.approve(
                request.getPaymentKey(),
                String.valueOf(order.getId()),
                request.getAmount()
        );

        if (!approveResult.isSuccess()) {
            payment.fail(approveResult.getFailureReason());
            paymentRepository.save(payment);
            throw new PaymentException.PaymentProcessingFailedException("PG사 결제 승인 실패: " + approveResult.getFailureReason());
        }

        // 5. 결제 엔티티 승인 완료 갱신
        payment.complete(approveResult.getTransactionId(), approveResult.getPaymentKey(), approveResult.getReceiptUrl());
        if (request.getPgProvider() != null) {
            payment.setPgProvider(request.getPgProvider());
        }
        String cardCo = request.getCardCompany() != null ? request.getCardCompany() : approveResult.getCardCompany();
        String cardNum = request.getCardNumber() != null ? request.getCardNumber() : approveResult.getCardNumber();
        Integer installment = request.getInstallmentMonths() != null ? request.getInstallmentMonths() : approveResult.getInstallmentMonths();
        if (cardCo != null || cardNum != null) {
            payment.updateCardInfo(cardCo, cardNum, installment);
        }
        Payment savedPayment = paymentRepository.save(payment);

        // 6. 주문 확정 처리 (Order: PENDING -> CONFIRMED)
        order.confirm();
        orderRepository.save(order);

        // 7. 결제 완료 및 주문 확정 이벤트 발행
        eventPublisher.publishEvent(new PaymentCompletedEvent(savedPayment));
        eventPublisher.publishEvent(new OrderConfirmedEvent(order));

        log.info("2단계 결제 승인 완료 - 결제 ID: {}, 주문 ID: {}, 승인 금액: {}",
                savedPayment.getId(), order.getId(), savedPayment.getAmount().getAmount());

        return PaymentResponse.from(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse completePayment(Long paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(paymentId));

        payment.complete(transactionId);
        Payment saved = paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentCompletedEvent(saved));
        return PaymentResponse.from(saved);
    }

    @Override
    @Transactional
    public PaymentResponse failPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(paymentId));

        payment.fail(reason);
        Payment saved = paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentFailedEvent(saved, reason));
        return PaymentResponse.from(saved);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(paymentId));

        if (payment.getPaymentKey() != null) {
            paymentGatewayClient.cancel(payment.getPaymentKey(), "고객 요청 취소", payment.getAmount().getAmount());
        }

        payment.cancel();
        Payment saved = paymentRepository.save(payment);

        // 연관 주문이 취소 가능 상태인 경우 자동 취소 동기화
        Order order = payment.getOrder();
        if (order != null && order.isCancellable()) {
            order.cancel("결제 취소 연동");
            orderRepository.save(order);
        }

        return PaymentResponse.from(saved);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(paymentId));

        if (payment.getPaymentKey() != null) {
            paymentGatewayClient.cancel(payment.getPaymentKey(), "환불 처리", refundAmount);
        }

        payment.refund(Money.of(refundAmount));
        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.from(saved);
    }
}
