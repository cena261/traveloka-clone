package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.iam.dto.response.TwoFactorSetupDto;
import com.cena.traveloka.iam.service.TwoFactorAuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/me/2fa")
@RequiredArgsConstructor
@Validated
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    @PostMapping("/setup")
    public ApiResponse<TwoFactorSetupDto> setupTwoFactorAuth(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Setup 2FA request");

        TwoFactorSetupDto setupDto = twoFactorAuthService.setupTwoFactorAuth(token);

        return ApiResponse.success(
                "2FA setup initiated. Please scan the QR code with your authenticator app and save the backup codes securely.",
                setupDto
        );
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verifyTwoFactorAuth(
            @RequestParam @NotBlank(message = "2FA code is required")
            @Size(min = 6, max = 6, message = "2FA code must be 6 digits") String code,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Verify 2FA request");

        twoFactorAuthService.verifyAndActivateTwoFactorAuth(token, code);

        return ApiResponse.success(
                "2FA enabled successfully. Your account is now protected with two-factor authentication.",
                null
        );
    }

    @PostMapping("/disable")
    public ApiResponse<Void> disableTwoFactorAuth(
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String backupCode,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Disable 2FA request");

        if ((password == null || password.isBlank()) && (backupCode == null || backupCode.isBlank())) {
            throw new IllegalArgumentException("Either password or backup code is required to disable 2FA");
        }

        twoFactorAuthService.disableTwoFactorAuth(token, password, backupCode);

        return ApiResponse.success(
                "2FA disabled successfully",
                null
        );
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }
}
