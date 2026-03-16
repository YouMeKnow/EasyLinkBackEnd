package com.easylink.easylink.vibe_service.application.dto;

import com.easylink.easylink.vibe_service.domain.interaction.offer.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import java.time.Instant;

@Getter
@Setter
public class OfferDto {
    private UUID id;
    private UUID vibeId;
    private String title;
    private String description;
    private DiscountType discountType;
    private int initialDiscount;
    private int currentDiscount;
    private int decreaseStep;
    private int decreaseIntervalMinutes;
    private boolean active;
    private Instant startTime;
    private Instant endTime;
}
