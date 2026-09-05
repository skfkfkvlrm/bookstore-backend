package com.example.spring.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectApprovalRequest {
    @NotBlank(message = "반려 사유는 필수입니다.")
    private String reason;
}