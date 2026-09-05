package com.example.spring.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 2단계 PG 결제 승인 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConfirmRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    @NotBlank(message = "결제 키(paymentKey)는 필수입니다.")
    private String paymentKey;

    @NotNull(message = "결제 금액은 필수입니다.")
    @Positive(message = "결제 금액은 양수여야 합니다.")
    private BigDecimal amount;

    // PG사 정보 (기본값: TOSS_PAYMENTS)
    @Builder.Default
    private String pgProvider = "TOSS_PAYMENTS";

    // 카드 또는 간편결제 제공사 (선택)
    private String cardCompany;
    private String cardNumber;
    private Integer installmentMonths;
}
