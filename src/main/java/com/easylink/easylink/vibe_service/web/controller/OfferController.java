package com.easylink.easylink.vibe_service.web.controller;

import com.easylink.easylink.vibe_service.application.dto.CreateOfferCommand;
import com.easylink.easylink.vibe_service.application.dto.OfferDto;
import com.easylink.easylink.vibe_service.application.service.OfferServiceImpl;
import com.easylink.easylink.vibe_service.web.dto.CreateOfferRequest;
import com.easylink.easylink.vibe_service.web.dto.OfferPatchRequest;
import com.easylink.easylink.vibe_service.web.dto.OfferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferServiceImpl offerService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(
            @Valid @RequestBody CreateOfferRequest createOfferRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        OfferDto offerDto = offerService.create(modelMapper.map(createOfferRequest, CreateOfferCommand.class));
        OfferResponse res = modelMapper.map(offerDto, OfferResponse.class);

        UUID me = currentVibeId(jwt);
        res.setCanManage(me != null && me.equals(res.getVibeId()));

        return ResponseEntity.ok(res);
    }

    @GetMapping("/vibe/{vibeId}")
    public ResponseEntity<List<OfferResponse>> getOffersByVibeId(
            @PathVariable UUID vibeId,
            @AuthenticationPrincipal Jwt jwt
    ){
        List<OfferDto> offerDtoList = offerService.findAllById(vibeId);

        UUID me = currentVibeId(jwt);

        List<OfferResponse> list = offerDtoList.stream()
                .map(dto -> {
                    OfferResponse r = modelMapper.map(dto, OfferResponse.class);
                    r.setCanManage(me != null && me.equals(r.getVibeId()));
                    return r;
                })
                .toList();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> getOfferById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ){
        OfferDto offerDto = offerService.findOfferById(id);
        OfferResponse res = modelMapper.map(offerDto, OfferResponse.class);

        UUID me = currentVibeId(jwt);
        res.setCanManage(me != null && me.equals(res.getVibeId()));

        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateOffer(
            @PathVariable UUID id,
            @Valid @RequestBody OfferPatchRequest patch,
            @AuthenticationPrincipal Jwt jwt
    ){
        offerService.updateOffer(id, patch, jwt);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> deleteOffer(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        offerService.deleteOffer(offerId, jwt);
        return ResponseEntity.noContent().build();
    }

    private UUID currentVibeId(Jwt jwt) {
        if (jwt == null) return null;
        Object v = jwt.getClaims().get("vibeId");
        if (v == null) v = jwt.getClaims().get("vibe_id");
        if (v == null) v = jwt.getClaims().get("vibe");
        if (v == null) return null;
        try {
            return UUID.fromString(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}
