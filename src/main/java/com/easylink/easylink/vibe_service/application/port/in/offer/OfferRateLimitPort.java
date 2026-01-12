package com.easylink.easylink.vibe_service.application.port.in.offer;

public interface OfferRateLimitPort {
    boolean canCreateOffer(String userId);
    void incrementOffer(String vibeId);
    void decrementOffer(String userId);
}

