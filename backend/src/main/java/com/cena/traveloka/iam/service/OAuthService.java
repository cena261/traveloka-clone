package com.cena.traveloka.iam.service;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.OAuthProvider;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.mapper.UserMapper;
import com.cena.traveloka.iam.repository.OAuthProviderRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.cena.traveloka.iam.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuthService {

    private final OAuthProviderRepository oauthProviderRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final UserMapper userMapper;

    public AuthResponse handleOAuthLogin(
            String provider,
            String providerUserId,
            String email,
            String name,
            String avatarUrl,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt,
            JsonNode rawData,
            String ipAddress,
            String userAgent
    ) {
        Optional<OAuthProvider> existingOAuth = oauthProviderRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        User user;

        if (existingOAuth.isPresent()) {
            user = existingOAuth.get().getUser();

            OAuthProvider oauthProvider = existingOAuth.get();
            oauthProvider.setAccessToken(accessToken);
            oauthProvider.setRefreshToken(refreshToken);
            oauthProvider.setTokenExpiresAt(tokenExpiresAt);
            oauthProvider.setRawData(rawData);
            oauthProvider.setLastUsedAt(OffsetDateTime.now());
            oauthProviderRepository.save(oauthProvider);

            log.info("OAuth login successful for provider: {} and user: {}", provider, user.getId());
        } else {
            Optional<User> existingUser = userRepository.findByEmail(email);

            if (existingUser.isPresent()) {
                user = existingUser.get();
                createOAuthLink(user, provider, providerUserId, email, name, avatarUrl,
                               accessToken, refreshToken, tokenExpiresAt, rawData);

                log.info("OAuth provider {} linked to existing user: {}", provider, user.getId());
            } else {
                user = createUserFromOAuth(email, name, provider);
                createOAuthLink(user, provider, providerUserId, email, name, avatarUrl,
                               accessToken, refreshToken, tokenExpiresAt, rawData);

                log.info("New user created via OAuth provider: {}", provider);
            }
        }

        Authentication authentication = createAuthentication(user);
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(
                authentication,
                user.getId().toString(),
                user.getEmail()
        );
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        sessionService.createSession(user, jwtAccessToken, jwtRefreshToken, ipAddress, userAgent);

        UserDto userDto = userMapper.toDto(user);

        return AuthResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(jwtRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .user(userDto)
                .build();
    }

    public void linkOAuthProvider(
            UUID userId,
            String provider,
            String providerUserId,
            String email,
            String name,
            String avatarUrl,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt,
            JsonNode rawData
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (oauthProviderRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new RuntimeException("OAuth provider already linked: " + provider);
        }

        Optional<OAuthProvider> existingOAuth = oauthProviderRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingOAuth.isPresent()) {
            throw new RuntimeException("OAuth account already linked to another user");
        }

        createOAuthLink(user, provider, providerUserId, email, name, avatarUrl,
                       accessToken, refreshToken, tokenExpiresAt, rawData);

        log.info("OAuth provider {} linked to user: {}", provider, userId);
    }

    public void unlinkOAuthProvider(UUID userId, String provider) {
        if (!oauthProviderRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new RuntimeException("OAuth provider not linked: " + provider);
        }

        oauthProviderRepository.deleteByUserIdAndProvider(userId, provider);

        log.info("OAuth provider {} unlinked from user: {}", provider, userId);
    }

    @Transactional(readOnly = true)
    public List<OAuthProvider> getUserOAuthProviders(UUID userId) {
        return oauthProviderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isProviderLinked(UUID userId, String provider) {
        return oauthProviderRepository.existsByUserIdAndProvider(userId, provider);
    }

    public void updateOAuthTokens(
            UUID userId,
            String provider,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt
    ) {
        OAuthProvider oauthProvider = oauthProviderRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new RuntimeException("OAuth provider not linked"));

        oauthProvider.setAccessToken(accessToken);
        oauthProvider.setRefreshToken(refreshToken);
        oauthProvider.setTokenExpiresAt(tokenExpiresAt);
        oauthProviderRepository.save(oauthProvider);

        log.info("OAuth tokens updated for user: {} and provider: {}", userId, provider);
    }

    @Transactional(readOnly = true)
    public OAuthProvider getOAuthProvider(UUID userId, String provider) {
        return oauthProviderRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new RuntimeException("OAuth provider not found"));
    }


    private User createUserFromOAuth(String email, String name, String provider) {
        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        String username = email.split("@")[0] + "_" + provider;

        int suffix = 1;
        String finalUsername = username;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + "_" + suffix++;
        }

        User user = User.builder()
                .username(finalUsername)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .status(Status.active)
                .emailVerified(true) // OAuth emails are pre-verified
                .termsAcceptedAt(OffsetDateTime.now())
                .privacyAcceptedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private OAuthProvider createOAuthLink(
            User user,
            String provider,
            String providerUserId,
            String email,
            String name,
            String avatarUrl,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt,
            JsonNode rawData
    ) {
        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .name(name)
                .avatarUrl(avatarUrl)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(tokenExpiresAt)
                .rawData(rawData)
                .linkedAt(OffsetDateTime.now())
                .lastUsedAt(OffsetDateTime.now())
                .build();

        return oauthProviderRepository.save(oauthProvider);
    }

    public String getAuthorizationUrl(String provider) {
        return "https://" + provider + ".com/oauth/authorize";
    }

    public AuthResponse handleOAuthCallback(
            String provider,
            String code,
            String state,
            String ipAddress,
            String userAgent
    ) {
        throw new UnsupportedOperationException("handleOAuthCallback not yet implemented - requires OAuth client configuration");
    }

    private Authentication createAuthentication(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                authorities
        );
    }
}
