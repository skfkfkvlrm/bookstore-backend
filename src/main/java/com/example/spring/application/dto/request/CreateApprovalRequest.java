package com.example.spring.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApprovalRequest {

    private Long memberId;

    @NotBlank(message = "품의 제목은 필수입니다.")
    private String title;

    private String purpose;
    private String department;

    @NotEmpty(message = "최소 1권 이상의 도서 항목을 신청해야 합니다.")
    private List<ApprovalItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApprovalItemDto {
        private Long bookId;

        @NotBlank(message = "도서명은 필수입니다.")
        private String bookTitle;

        private String bookAuthor;
        private String isbn;
        private Integer quantity;
        private BigDecimal estimatedPrice;
    }
}