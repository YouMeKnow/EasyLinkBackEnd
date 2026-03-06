package com.easylink.easylink.vibe_service.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class PendingRequestResponse {
        UUID interactionId;
        UUID subscriberVibeId;
        String subscriberName;
        String subscriberDescription;
        String subscriberType;
        String subscriberPhoto;
        boolean anonymous;
}