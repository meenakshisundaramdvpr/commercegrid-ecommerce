package com.commercegrid.auth.dto;

import com.commercegrid.auth.enums.AdminStatus;
import com.commercegrid.auth.enums.StatusReasonCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminStatusUpdateRequest {

    @NotNull(message = "New status is required")
    @Schema(example = "LOCKED")
    private AdminStatus newStatus;

    @NotNull(message = "Reason code is required")
    @Schema(example = "SUSPICIOUS_ACTIVITY")
    private StatusReasonCode reasonCode;

    @NotBlank(message = "Status reason is required")
    @Size(max = 500)
    private String statusReason;
}