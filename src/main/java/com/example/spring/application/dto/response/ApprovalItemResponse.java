package com.example.spring.application.dto.response;

import com.example.spring.domain.model.PurchaseApprovalItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalItemResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String isbn;
    private Integer quantity;
    private BigDecimal estimatedPrice;
    private BigDecimal totalPrice;

    public static ApprovalItemResponse from(PurchaseApprovalItem item) {
        if (item == null) return null;
        return ApprovalItemResponse.builder()
                .id(item.getId())
                .bookId(item.getBookId())
                .bookTitle(item.getBookTitle())
                .bookAuthor(item.getBookAuthor())
                .isbn(item.getIsbn())
                .quantity(item.getQuantity())
                .estimatedPrice(item.getEstimatedPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}