package com.easylink.easylink.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.private:}")
    private Resource privateRes;

    @Value("${jwt.private-path:}")
    private String privatePath;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() throws Exception {
        byte[] pemBytes = null;
        if (privateRes != null && privateRes.exists()) {
            try (InputStream is = privateRes.getInputStream()) {
                pemBytes = is.readAllBytes();
            }
        }
        else if (privatePath != null && !privatePath.isBlank()) {
            pemBytes = Files.readAllBytes(Path.of(privatePath));
        }
        else {
            try (InputStream is = getClass().getResourceAsStream("/keys/private.pem")) {
                if (is != null) {
                    pemBytes = is.readAllBytes();
                }
            }
        }

        if (pemBytes == null) {
            throw new IllegalStateException(
                    "JWT private key not found. Provide one of:\n" +
                            " - env JWT_PRIVATE=file:/run/secrets/private.pem (preferred)\n" +
                            " - env JWT_PRIVATE=classpath:/keys/private.pem (dev)\n" +
                            " - env JWT_PRIVATE_PATH=/run/secrets/private.pem (fallback)\n" +
                            "Or add /keys/private.pem to classpath."
            );
        }

        String key = new String(pemBytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    public String generateToken(String id) {
        return Jwts.builder()
                .setSubject(id)
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}