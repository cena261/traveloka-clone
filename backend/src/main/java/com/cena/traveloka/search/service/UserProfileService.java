package com.cena.traveloka.search.service;

import com.cena.traveloka.search.entity.UserSearchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserProfileService {

    public UserSearchProfile getUserProfile(String userId) {
        // Mock implementation - in production, fetch from database
        log.debug("Getting user profile for: {}", userId);
        return null; // Return null to indicate no profile found
    }

    public void saveUserProfile(UserSearchProfile profile) {
        // Mock implementation - in production, save to database
        log.debug("Saving user profile for: {}", profile.getUserId());
    }
}