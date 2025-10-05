package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupDto {

    private String qrCode;

    private String secret;

    private List<String> backupCodes;

    @Builder.Default
    private String issuer = "Traveloka";

    private String accountName;
}
