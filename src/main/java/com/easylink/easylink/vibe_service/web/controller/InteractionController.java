package com.easylink.easylink.vibe_service.web.controller;

import com.easylink.easylink.vibe_service.application.dto.EarlyAccessRequestDTO;
import com.easylink.easylink.vibe_service.application.dto.InteractionWithOffersDTO;
import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.application.port.in.interaction.CreateEarlyAccessUseCase;
import com.easylink.easylink.vibe_service.application.port.in.interaction.CreateInteractionUseCase;
import com.easylink.easylink.vibe_service.application.service.InteractionService;
import com.easylink.easylink.vibe_service.domain.interaction.Interaction;
import com.easylink.easylink.vibe_service.web.dto.*;
import com.easylink.easylink.vibe_service.web.mapper.VibeResponseMapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final CreateInteractionUseCase createInteractionUseCase;
    private final InteractionService interactionService;
    private final CreateEarlyAccessUseCase createEarlyAccessUseCase;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<InteractionResponse> create(
            @RequestBody CreateInteractionRequest createInteractionRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                interactionService.createInteractionFromJwt(
                        createInteractionRequest,
                        jwt.getSubject()
                )
        );
    }

    @GetMapping("/subscribers/{vibeId}")
    public List<VibeDto> subscribers(@PathVariable UUID vibeId, @AuthenticationPrincipal Jwt jwt) {
        return interactionService.getSubscribersOwnedBy(vibeId, jwt.getSubject());
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<List<VibeResponse>> getFollowing(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){

        List<VibeDto> vibeDtoList = interactionService.getFollowing(id);
        List<VibeResponse> vibeResponseList = vibeDtoList.stream().map(val->VibeResponseMapper.toResponse(val)).toList();

        return ResponseEntity.ok(vibeResponseList);
    }

    @GetMapping("/{id}/following-offer")
    public ResponseEntity<List<InteractionWithOfferResponse>> getFollowingWithOffers(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){

        List<InteractionWithOffersDTO> interactionWithOffersDTO = interactionService.getFollowingWithOffers(id);

        //   List<InteractionWithOfferResponse> interactionWithOfferResponse = interactionWithOffersDTO.stream().map(dto-> modelMapper.map(dto, InteractionWithOfferResponse.class)).toList();
        List<InteractionWithOfferResponse> responses = interactionWithOffersDTO.stream()
                .map(dto -> {
                    Interaction i = dto.interaction();
                    return new InteractionWithOfferResponse(
                            i.getId(),
                            i.getTargetVibe().getId(),
                            i.getTargetVibe().getName(),
                            i.getTargetVibe().getType().toString(),
                            i.getTargetVibe().getDescription(),
                            dto.count()
                    );
                })
                .toList();


        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{subscriberVibeId}/subscribed")
    public ResponseEntity<Boolean> isSubscribed(@PathVariable UUID subscriberVibeId, @RequestParam UUID targetVibeId, @AuthenticationPrincipal Jwt jwt){

        Boolean isSubscribed = interactionService.isSubscribed(subscriberVibeId,targetVibeId);

        return ResponseEntity.ok(isSubscribed);
    }

    @PostMapping("/early-access")
    public ResponseEntity<EarlyRequestAccessResponse> requestEarlySubscription(@RequestParam String email, @AuthenticationPrincipal Jwt jwt){
        EarlyAccessRequestDTO earlyAccessRequestDTO= createEarlyAccessUseCase.create(email);
        EarlyRequestAccessResponse earlyRequestAccessResponse = modelMapper.map(earlyAccessRequestDTO,EarlyRequestAccessResponse.class);
        return ResponseEntity.ok(earlyRequestAccessResponse);
    }

    @GetMapping("/early-access/status")
    public ResponseEntity<Boolean> isSubscribedEarlyAccess(@RequestParam String email, @AuthenticationPrincipal Jwt jwt){

        boolean isSubscribed = interactionService.isSubscribedEarlyAccess(email);

        return ResponseEntity.ok(isSubscribed);
    }

//    @RequestMapping("/followers")
//    public ResponseEntity<VibeResponse> getFollowers(@RequestParam UUID vibeId,@AuthenticationPrincipal Jwt jwt){
//
//
//
//        return ResponseEntity.ok(null);
//    }

}
