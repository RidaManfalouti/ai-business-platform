package com.aibi.analytics_service.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class InternalTokenService {

    private static final Logger log = LoggerFactory.getLogger(InternalTokenService.class);
    private final byte[] secretBytes;

    public InternalTokenService(@Value("${jwt.secret}") String jwtSecret) {
        this.secretBytes = Base64.getDecoder().decode(jwtSecret);
    }

    public String getInternalServiceToken() {
        try {
            MACSigner signer = new MACSigner(secretBytes);
            Date now = new Date();
            Date expiry = new Date(now.getTime() + 300_000); // 5 minutes

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject("system-analytics-service")
                    .issuer("analytics-service")
                    .claim("roles", List.of("ROLE_ADMIN", "ROLE_USER"))
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception ex) {
            log.error("Échec de la génération du jeton JWT interne de service : {}", ex.getMessage());
            throw new IllegalStateException("Impossible de générer le jeton interne de service", ex);
        }
    }
}
