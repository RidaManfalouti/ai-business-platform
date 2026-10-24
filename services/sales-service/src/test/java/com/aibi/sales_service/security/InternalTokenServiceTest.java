package com.aibi.sales_service.security;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTokenServiceTest {

    private static final String TEST_SECRET = "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=";

    @Test
    @DisplayName("getInternalServiceToken : Génère un JWT valide avec rôle ROLE_ADMIN et subject system-saga-recovery")
    void getInternalServiceToken_GeneratesValidJwt() throws Exception {
        InternalTokenService tokenService = new InternalTokenService(TEST_SECRET);

        String token = tokenService.getInternalServiceToken();
        assertThat(token).isNotBlank();

        SignedJWT parsedJwt = SignedJWT.parse(token);
        assertThat(parsedJwt.getJWTClaimsSet().getSubject()).isEqualTo("system-saga-recovery");
        assertThat(parsedJwt.getJWTClaimsSet().getIssuer()).isEqualTo("sales-service");

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) parsedJwt.getJWTClaimsSet().getClaim("roles");
        assertThat(roles).contains("ROLE_ADMIN");
        assertThat(parsedJwt.getJWTClaimsSet().getExpirationTime()).isNotNull();
    }
}
