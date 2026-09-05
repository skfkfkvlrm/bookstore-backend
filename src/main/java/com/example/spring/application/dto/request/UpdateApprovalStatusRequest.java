package com.example.spring.application.dto.request;

import com.example.spring.domain.model.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateApprovalStatusRequest {
    @NotNull(message = "변경할 상태는 필수입니다.")
    private ApprovalStatus status;
    private String reason;
}