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
public class PaymentCancelResult {
    private String paymentKey;
    private String transactionId;
    private BigDecimal cancelAmount;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private boolean success;
    private String failureReason;
}
