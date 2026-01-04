package com.easylink.easylink.vibe_service.web.dto;

import com.easylink.easylink.vibe_service.domain.model.VibeType;
import lombok.Getter;
import lombok.Setter;
import com.easylink.easylink.vibe_service.web.dto.MiniVibeResponse;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class VibeResponse {
    private UUID id;
    private String description;
    private VibeType type;
    private String name;
    private Boolean visible;
    private String publicCode;
    private String photo;
    private List<VibeFieldDTO> fieldsDTO;
    private Long subscriberCount;
    private List<MiniVibeResponse> subscriberVibes;
}
