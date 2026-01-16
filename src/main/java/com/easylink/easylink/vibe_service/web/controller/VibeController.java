package com.easylink.easylink.vibe_service.web.controller;

import com.easylink.easylink.vibe_service.application.dto.UpdateVibeCommand;
import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.application.port.in.vibe.*;
import com.easylink.easylink.vibe_service.web.dto.CreateVibeRequest;
import com.easylink.easylink.vibe_service.web.dto.UpdateVibeRequest;
import com.easylink.easylink.vibe_service.web.dto.VibeResponse;
import com.easylink.easylink.vibe_service.web.mapper.VibeRequestMapper;
import com.easylink.easylink.vibe_service.web.mapper.VibeResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/vibes")
@Tag(name="Vibe API", description = "Manage Vibe profiles")
@RequiredArgsConstructor
public class VibeController {

    private final CreateVibeUseCase createVibeUseCase;
    private final UpdateVibeUseCase updateVibeUseCase;
    private final DeleteVibeUseCase deleteVibeUseCase;
    private final GetVibeUseCase getVibeUseCase;
    private final GetVibeByIdUseCase getVibeByIdUseCase;

    @Operation(summary = "Create Vibe", description = "Create new Vibe based on title and fields")
    @PostMapping
    public ResponseEntity<VibeResponse> create (@RequestBody CreateVibeRequest request, @AuthenticationPrincipal Jwt jwt){

        //    UUID accountId = UUID.fromString(jwt.getSubject());
        String vibAccountId = jwt.getSubject();

        VibeDto vibeDto = createVibeUseCase.create(VibeRequestMapper.toCommand(request),vibAccountId);

        return ResponseEntity.ok(VibeResponseMapper.toResponse(vibeDto));
    }

    @Operation(summary = "Update profile", description = "Update Vibe profile using ID")
    @PutMapping("/{id}")
    public ResponseEntity<VibeResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateVibeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID accountId = UUID.fromString(jwt.getSubject());

        VibeDto vibeDto = updateVibeUseCase.update(request,id,accountId);

        return ResponseEntity.ok(VibeResponseMapper.toResponse(vibeDto));
    }
    @Operation(summary = "Delete profile", description = "Delete Vibe profile by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt){
        UUID accountId = UUID.fromString(jwt.getSubject());

        deleteVibeUseCase.delete(id,accountId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all profile", description = "Get all Vibe profiles for current accountId")
    @GetMapping
    public ResponseEntity<List<VibeResponse>> getAll(@AuthenticationPrincipal Jwt jwt){

        String id = jwt.getSubject();

        List<VibeDto> vibeDtoList = getVibeUseCase.findAllByAccountId(UUID.fromString(id));

        return ResponseEntity.ok((vibeDtoList.stream().map(VibeResponseMapper::toResponse)).toList());
    }

    @Operation(summary = "Get profile", description = "Get Vibe profile using ID")
    @GetMapping("/{id}")
    public ResponseEntity<VibeResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ){
        String viewerAccountId = (jwt != null ? jwt.getSubject() : null);
        //      UUID accountId = UUID.fromString(jwt.getSubject());

        VibeDto vibeDto = getVibeByIdUseCase.getVibeById(id, viewerAccountId);

        return ResponseEntity.ok(VibeResponseMapper.toResponse(vibeDto));
    }
}

