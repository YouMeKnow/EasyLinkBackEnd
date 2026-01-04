package com.easylink.easylink.vibe_service.application.dto;

import com.easylink.easylink.vibe_service.domain.model.VibeType;
import com.easylink.easylink.vibe_service.web.dto.VibeFieldDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class VibeDto {
    private UUID id;
    private String description;
    private VibeType type;
    private String name;
    private Boolean visible;
    private String publicCode;
    private String photo;
    private List<VibeFieldDTO> fieldsDTO;
    private Long subscriberCount;
    private List<MiniVibeDto> subscriberVibes;
}
