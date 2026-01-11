package com.easylink.easylink.vibe_service.web.dto;

import com.easylink.easylink.vibe_service.domain.interaction.offer.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OfferPatchRequest {

    @Size(max = 80, message = "title must be at most 80 characters")
    private String title;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    private DiscountType discountType;

    @Min(0) @Max(1_000_000)
    private Integer initialDiscount;

    @Min(0) @Max(1_000_000)
    private Integer currentDiscount;

    @Min(0) @Max(1_000_000)
    private Integer decreaseStep;

    @Min(0) @Max(10080)
    private Integer decreaseIntervalMinutes;

    private Boolean active;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @AssertTrue(message = "endTime must be after startTime")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) return true;
        return endTime.isAfter(startTime);
    }
}
