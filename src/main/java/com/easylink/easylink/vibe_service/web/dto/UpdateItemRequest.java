package com.easylink.easylink.vibe_service.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateItemRequest {

    // null allowed (partial update)
    @Size(min = 1, max = 80, message = "Title must be between 1 and 80 characters")
    private String title;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "Price must be >= 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have up to 10 digits and 2 decimals")
    private BigDecimal price;

    @Size(max = 2048, message = "Image URL must be at most 2048 characters")
    private String imageUrl;
}
