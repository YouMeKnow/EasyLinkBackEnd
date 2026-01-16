package com.easylink.easylink.services;

import com.easylink.easylink.entities.Email2faChallenge;
import com.easylink.easylink.entities.VibeAccount;
import com.easylink.easylink.repositories.Email2faChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Email2faService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private final Email2faChallengeRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public UUID createChallenge(VibeAccount acc, String ip, String userAgent) {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        String hash = passwordEncoder.encode(code);

        Email2faChallenge ch = new Email2faChallenge();
        ch.setAccount(acc);
        ch.setCodeHash(hash);
        ch.setExpiresAt(Instant.now().plus(CODE_TTL));
        ch.setAttempts(0);
        ch.setCreatedAt(Instant.now());
        ch.setIp(ip);
        ch.setUserAgent(userAgent);

        ch = repo.save(ch);


        emailVerificationService.sendLoginCodeEmail(acc, code);

        return ch.getId();
    }

    public VibeAccount verify(UUID challengeId, String code) {
        Email2faChallenge ch = repo.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code"));

        if (ch.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
        }
        if (Instant.now().isAfter(ch.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
        }
        if (ch.getAttempts() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
        }

        ch.setAttempts(ch.getAttempts() + 1);

        boolean ok = passwordEncoder.matches(code, ch.getCodeHash());
        if (!ok) {
            repo.save(ch);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
        }

        ch.setUsedAt(Instant.now());
        repo.save(ch);

        return ch.getAccount();
    }
}
