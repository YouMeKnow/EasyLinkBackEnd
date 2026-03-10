package com.easylink.easylink.ai.dto;

import com.easylink.easylink.enums.IntentType;

public record IntentResult(
        IntentType intent,
        String category
        ) {}
