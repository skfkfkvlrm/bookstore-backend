package com.example.spring.infrastructure.payment;

import java.math.BigDecimal;

public interface PaymentGatewayClient {
    /**
     * PG 결제 2단계 최종 승인 요청
     */
    PaymentApproveResult approve(String paymentKey, String orderId, BigDecimal amount);

    /**
     * PG 결제 취소/환불 요청
     */
    PaymentCancelResult cancel(String paymentKey, String reason, BigDecimal cancelAmount);
}
