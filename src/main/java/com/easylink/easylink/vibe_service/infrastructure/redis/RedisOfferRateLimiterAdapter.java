package com.easylink.easylink.vibe_service.infrastructure.redis;

import com.easylink.easylink.vibe_service.application.port.in.offer.OfferRateLimitPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisOfferRateLimiterAdapter implements OfferRateLimitPort {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_OFFERS = 5;

    private String key(String vibeId) {
        return "limit:offers:" + vibeId;
    }

    private long getCount(String vibeId) {
        String v = redisTemplate.opsForValue().get(key(vibeId));
        if (v == null) return 0;
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }
    @Override
    public boolean canCreateOffer(String vibeId) {
        return getCount(vibeId) < MAX_OFFERS;
    }

    public void incrementOffer(String vibeId) {
        Long count = redisTemplate.opsForValue().increment(key(vibeId));
        if (count != null && count == 1) {
            redisTemplate.expire(key(vibeId), Duration.ofDays(365));
        }
    }

    @Override
    public void decrementOffer(String vibeId) {
        Long count = redisTemplate.opsForValue().decrement(key(vibeId));
        if (count != null && count <= 0) {
            redisTemplate.delete(key(vibeId)); // чтобы не было -1, -2
        }
    }
}
