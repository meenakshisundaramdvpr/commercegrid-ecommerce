package com.commercegrid.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminLoginResponse {
    @Builder.Default
    private String message = "Admin login successful";

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private AdminResponse admin;
}