package com.example.spring.infrastructure.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 로컬 테스트 및 샌드박스용 Mock PG 클라이언트
 */
@Slf4j
@Component("mockPaymentGatewayClient")
public class MockPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentApproveResult approve(String paymentKey, String orderId, BigDecimal amount) {
        log.info("[MockPG] 결제 승인 시뮬레이션 - paymentKey: {}, orderId: {}, amount: {}", paymentKey, orderId, amount);

        String transactionId = "TX_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String receiptUrl = "https://dashboard.tosspayments.com/receipt/mock/" + paymentKey;

        return PaymentApproveResult.builder()
                .transactionId(transactionId)
                .paymentKey(paymentKey != null ? paymentKey : "mock_pk_" + UUID.randomUUID().toString().substring(0, 12))
                .orderId(orderId)
                .amount(amount)
                .method("CREDIT_CARD")
                .pgProvider("MOCK_PG")
                .receiptUrl(receiptUrl)
                .approvedAt(LocalDateTime.now())
                .cardCompany("현대카드")
                .cardNumber("5424-12**-****-8823")
                .installmentMonths(0)
                .success(true)
                .build();
    }

    @Override
    public PaymentCancelResult cancel(String paymentKey, String reason, BigDecimal cancelAmount) {
        log.info("[MockPG] 결제 취소/환불 시뮬레이션 - paymentKey: {}, reason: {}, amount: {}", paymentKey, reason, cancelAmount);

        return PaymentCancelResult.builder()
                .paymentKey(paymentKey)
                .transactionId("CANCEL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .cancelAmount(cancelAmount)
                .cancelReason(reason)
                .cancelledAt(LocalDateTime.now())
                .success(true)
                .build();
    }
}
