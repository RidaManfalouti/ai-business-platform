package com.aibi.sales_service.security;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTokenServiceTest {

    private static final String TEST_SECRET = "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=";
    private static final UUID TEST_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("getInternalServiceToken : Génère un JWT valide avec rôle ROLE_ADMIN, subject system-saga-recovery et tenant_id")
    void getInternalServiceToken_GeneratesValidJwt() throws Exception {
        InternalTokenService tokenService = new InternalTokenService(TEST_SECRET);

        String token = tokenService.getInternalServiceToken(TEST_TENANT_ID);
        assertThat(token).isNotBlank();

        SignedJWT parsedJwt = SignedJWT.parse(token);
        assertThat(parsedJwt.getJWTClaimsSet().getSubject()).isEqualTo("system-saga-recovery");
        assertThat(parsedJwt.getJWTClaimsSet().getIssuer()).isEqualTo("sales-service");
        assertThat(parsedJwt.getJWTClaimsSet().getStringClaim("tenant_id")).isEqualTo(TEST_TENANT_ID.toString());

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) parsedJwt.getJWTClaimsSet().getClaim("roles");
        assertThat(roles).contains("ROLE_ADMIN");
        assertThat(parsedJwt.getJWTClaimsSet().getExpirationTime()).isNotNull();
    }
}
