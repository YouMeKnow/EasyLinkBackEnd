package com.easylink.easylink.services;

import com.easylink.easylink.entities.RefreshToken;
import com.easylink.easylink.entities.VibeAccount;
import com.easylink.easylink.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Duration REFRESH_TTL_REMEMBER = Duration.ofDays(30);
    private static final Duration REFRESH_TTL_NORMAL = Duration.ofDays(1);

    private final RefreshTokenRepository repo;

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String issueRefreshToken(VibeAccount acc, boolean rememberMe, String ip, String userAgent) {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        String hash = sha256(raw);

        RefreshToken rt = new RefreshToken();
        rt.setAccount(acc);
        rt.setTokenHash(hash);
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plus(rememberMe ? REFRESH_TTL_REMEMBER : REFRESH_TTL_NORMAL));
        rt.setIp(ip);
        rt.setUserAgent(userAgent);

        repo.save(rt);
        return raw;
    }

    public VibeAccount consumeForAccess(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);

        RefreshToken rt = repo.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (Instant.now().isAfter(rt.getExpiresAt())) {
            rt.setRevokedAt(Instant.now());
            repo.save(rt);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        return rt.getAccount();
    }

    public void revoke(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        repo.findByTokenHashAndRevokedAtIsNull(hash).ifPresent(rt -> {
            rt.setRevokedAt(Instant.now());
            repo.save(rt);
        });
    }
}
