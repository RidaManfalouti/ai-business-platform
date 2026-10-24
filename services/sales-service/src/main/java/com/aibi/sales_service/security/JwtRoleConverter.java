package com.aibi.sales_service.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalClaimValue = jwt.getClaimAsString(JwtClaimNames.SUB);
        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim == null) {
            rolesClaim = jwt.getClaim("authorities");
        }

        if (rolesClaim instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    addAuthority(authorities, item.toString());
                }
            }
        } else if (rolesClaim instanceof String str) {
            for (String part : str.split("[,\\s]+")) {
                addAuthority(authorities, part);
            }
        }

        return Collections.unmodifiableList(authorities);
    }

    private void addAuthority(List<GrantedAuthority> authorities, String rawRole) {
        if (rawRole != null) {
            String trimmed = rawRole.trim();
            if (!trimmed.isEmpty()) {
                if (!trimmed.startsWith("ROLE_")) {
                    trimmed = "ROLE_" + trimmed;
                }
                authorities.add(new SimpleGrantedAuthority(trimmed));
            }
        }
    }
}
