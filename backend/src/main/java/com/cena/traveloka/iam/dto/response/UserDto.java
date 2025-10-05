package com.cena.traveloka.iam.dto.response;

import com.cena.traveloka.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String displayName;

    private String avatarUrl;

    private Status status;

    private Boolean emailVerified;

    private Boolean phoneVerified;

    private Boolean twoFactorEnabled;
}
