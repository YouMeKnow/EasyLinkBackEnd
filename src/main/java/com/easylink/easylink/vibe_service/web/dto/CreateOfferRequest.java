package com.easylink.easylink.vibe_service.web.dto;

import com.easylink.easylink.vibe_service.domain.interaction.offer.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CreateOfferRequest {

    @NotNull(message = "vibeId is required")
    private UUID vibeId;

    @NotBlank(message = "title is required")
    @Size(max = 80, message = "title must be at most 80 characters")
    private String title;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @NotNull(message = "discountType is required")
    private DiscountType discountType;

    @Min(value = 0, message = "initialDiscount must be >= 0")
    @Max(value = 1_000_000, message = "initialDiscount is too large")
    private int initialDiscount;

    @Min(value = 0, message = "currentDiscount must be >= 0")
    @Max(value = 1_000_000, message = "currentDiscount is too large")
    private int currentDiscount;

    @Min(value = 0, message = "decreaseStep must be >= 0")
    @Max(value = 1_000_000, message = "decreaseStep is too large")
    private int decreaseStep;

    @Min(value = 0, message = "decreaseIntervalMinutes must be >= 0")
    @Max(value = 10080, message = "decreaseIntervalMinutes is too large")
    private int decreaseIntervalMinutes;

    private boolean active;

    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    private LocalDateTime endTime;

    @AssertTrue(message = "endTime must be after startTime")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) return true; // @NotNull поймает отдельно
        return endTime.isAfter(startTime);
    }
}
