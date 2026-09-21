package com.commercegrid.auth.dto;

import com.commercegrid.auth.enums.AdminStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminStatusUpdateRequest {

    @NotNull(message = "New status is required")
    @Schema(example = "LOCKED")
    private AdminStatus newStatus;

    @NotBlank(message = "Status reason is required")
    @Schema(example = "Suspicious login pattern detected — pending security review")
    private String statusReason;
}