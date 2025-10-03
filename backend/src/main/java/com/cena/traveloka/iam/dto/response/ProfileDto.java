package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * T038: ProfileDto
 * User profile information DTO (FR-019).
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - DTO separates from entity
 * - Used in UserDetailDto and profile endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    /**
     * Middle name.
     */
    private String middleName;

    /**
     * Nickname.
     */
    private String nickname;

    /**
     * Bio/description.
     */
    private String bio;

    /**
     * Occupation.
     */
    private String occupation;

    /**
     * Company name.
     */
    private String company;

    /**
     * Personal website URL.
     */
    private String website;

    /**
     * Preferred airlines.
     */
    private List<String> preferredAirlines;

    /**
     * Preferred hotels.
     */
    private List<String> preferredHotels;

    /**
     * Dietary restrictions.
     */
    private List<String> dietaryRestrictions;

    /**
     * Special needs/requirements.
     */
    private String specialNeeds;

    /**
     * Total bookings count.
     */
    private Integer totalBookings;

    /**
     * Total amount spent.
     */
    private Double totalSpent;

    /**
     * Loyalty points.
     */
    private Integer loyaltyPoints;

    /**
     * Loyalty tier.
     */
    private String loyaltyTier;

    /**
     * Profile completion percentage.
     */
    private Integer profileCompletionPercentage;
}
