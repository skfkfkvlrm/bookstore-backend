package com.example.spring.infrastructure.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentApproveResult {
    private String transactionId;
    private String paymentKey;
    private String orderId;
    private BigDecimal amount;
    private String method;
    private String pgProvider;
    private String receiptUrl;
    private LocalDateTime approvedAt;
    private String cardCompany;
    private String cardNumber;
    private Integer installmentMonths;
    private boolean success;
    private String failureReason;
}
