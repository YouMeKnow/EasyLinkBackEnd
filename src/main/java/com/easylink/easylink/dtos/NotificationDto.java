package com.easylink.easylink.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class NotificationDto {
    UUID id;
    String type;
    String title;
    String body;
    String link;
    boolean read;
    OffsetDateTime createdAt;
}