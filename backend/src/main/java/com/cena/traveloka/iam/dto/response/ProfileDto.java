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
public class ProfileDto {

    private String middleName;

    private String nickname;

    private String bio;

    private String occupation;

    private String company;

    private String website;

    private List<String> preferredAirlines;

    private List<String> preferredHotels;

    private List<String> dietaryRestrictions;

    private String specialNeeds;

    private Integer totalBookings;

    private Double totalSpent;

    private Integer loyaltyPoints;

    private String loyaltyTier;

    private Integer profileCompletionPercentage;
}
