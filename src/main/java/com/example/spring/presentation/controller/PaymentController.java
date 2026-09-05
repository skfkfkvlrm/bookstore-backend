package com.example.spring.presentation.controller;

import com.example.spring.application.PaymentService;
import com.example.spring.application.dto.request.PaymentConfirmRequest;
import com.example.spring.application.dto.response.PaymentResponse;
import com.example.spring.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 결제 관리 REST API Controller
 */
@Tag(name = "Payment", description = "결제 관리 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 2단계 결제 최종 승인 (위변조 검증 및 PG사 연동 승인)
     */
    @Operation(summary = "2단계 결제 최종 승인", description = "주문 금액 위변조를 검증하고 PG사에 결제 최종 승인을 요청합니다.")
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request) {
        log.info("결제 최종 승인 요청 - 주문 ID: {}, 결제 키: {}, 금액: {}",
                request.getOrderId(), request.getPaymentKey(), request.getAmount());
        PaymentResponse payment = paymentService.confirmPayment(request);
        return ResponseEntity.ok(payment);
    }

    /**
     * 결제 ID로 조회
     */
    @Operation(summary = "결제 ID로 조회")
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        log.debug("결제 조회 요청 - ID: {}", id);
        PaymentResponse payment = paymentService.findById(id);
        return ResponseEntity.ok(payment);
    }

    /**
     * 주문 ID로 결제 조회
     */
    @Operation(summary = "주문 ID로 결제 조회")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        log.debug("주문별 결제 조회 요청 - Order ID: {}", orderId);
        PaymentResponse payment = paymentService.findByOrderId(orderId);
        return ResponseEntity.ok(payment);
    }

    /**
     * 상태별 결제 목록 조회
     */
    @Operation(summary = "상태별 결제 목록 조회")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        log.debug("상태별 결제 조회 요청 - 상태: {}", status);
        List<PaymentResponse> payments = paymentService.findByStatus(status);
        return ResponseEntity.ok(payments);
    }

    /**
     * 결제 완료 수동 처리
     */
    @Operation(summary = "결제 완료 처리")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<PaymentResponse> completePayment(
            @PathVariable Long id,
            @RequestParam String transactionId) {
        log.info("결제 완료 처리 요청 - ID: {}, Transaction ID: {}", id, transactionId);
        PaymentResponse payment = paymentService.completePayment(id, transactionId);
        return ResponseEntity.ok(payment);
    }

    /**
     * 결제 실패 처리
     */
    @Operation(summary = "결제 실패 처리")
    @PatchMapping("/{id}/fail")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        log.info("결제 실패 처리 요청 - ID: {}, 사유: {}", id, reason);
        PaymentResponse payment = paymentService.failPayment(id, reason);
        return ResponseEntity.ok(payment);
    }

    /**
     * 결제 취소
     */
    @Operation(summary = "결제 취소")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long id) {
        log.info("결제 취소 요청 - ID: {}", id);
        PaymentResponse payment = paymentService.cancelPayment(id);
        return ResponseEntity.ok(payment);
    }

    /**
     * 결제 환불
     */
    @Operation(summary = "결제 환불")
    @PatchMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal refundAmount) {
        log.info("결제 환불 요청 - ID: {}, 환불 금액: {}", id, refundAmount);
        PaymentResponse payment = paymentService.refundPayment(id, refundAmount);
        return ResponseEntity.ok(payment);
    }

    /**
     * PG사 비동기 웹훅 수신 엔드포인트
     */
    @Operation(summary = "PG사 비동기 웹훅 수신")
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody(required = false) Map<String, Object> webhookData) {
        log.info("PG사 비동기 웹훅 수신: {}", webhookData);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "웹훅 정상 처리 완료"
        ));
    }
}
