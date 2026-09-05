package com.example.spring.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_approval_items")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseApprovalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", nullable = false)
    @JsonBackReference
    private PurchaseApproval approval;

    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false, length = 200)
    private String bookTitle;

    @Column(length = 100)
    private String bookAuthor;

    @Column(length = 30)
    private String isbn;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal estimatedPrice;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    void attachToApproval(PurchaseApproval approval) {
        this.approval = approval;
    }
}