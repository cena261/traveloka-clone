package com.cena.traveloka.iam.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Collection<String> realmRoles = extractRealmRoles(jwt);
        authorities.addAll(realmRoles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toList()));

        Collection<String> resourceRoles = extractResourceRoles(jwt);
        authorities.addAll(resourceRoles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toList()));

        return authorities;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);

        if (realmAccess != null && realmAccess.containsKey(ROLES_CLAIM)) {
            Object rolesObj = realmAccess.get(ROLES_CLAIM);
            if (rolesObj instanceof Collection) {
                return (Collection<String>) rolesObj;
            }
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractResourceRoles(Jwt jwt) {
        List<String> roles = new ArrayList<>();
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);

        if (resourceAccess != null) {
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> clientAccess = (Map<String, Object>) entry.getValue();
                    if (clientAccess.containsKey(ROLES_CLAIM)) {
                        Object rolesObj = clientAccess.get(ROLES_CLAIM);
                        if (rolesObj instanceof Collection) {
                            roles.addAll((Collection<String>) rolesObj);
                        }
                    }
                }
            }
        }

        return roles;
    }
}
