package com.easylink.easylink.vibe_service.application.service;

import com.easylink.easylink.exceptions.OfferLimitExceededException;
import com.easylink.easylink.services.NotificationService;
import com.easylink.easylink.vibe_service.application.dto.CreateOfferCommand;
import com.easylink.easylink.vibe_service.application.dto.OfferDto;
import com.easylink.easylink.vibe_service.application.port.in.offer.CreateOfferUseCase;
import com.easylink.easylink.vibe_service.application.port.in.offer.OfferRateLimitPort;
import com.easylink.easylink.vibe_service.application.port.out.VibeRepositoryPort;
import com.easylink.easylink.vibe_service.domain.interaction.offer.DiscountType;
import com.easylink.easylink.vibe_service.domain.interaction.offer.Offer;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import com.easylink.easylink.vibe_service.domain.model.VibeType;
import com.easylink.easylink.vibe_service.infrastructure.exception.OfferUpdateException;
import com.easylink.easylink.vibe_service.infrastructure.repository.JpaInteractionRepositoryAdapter;
import com.easylink.easylink.vibe_service.infrastructure.repository.JpaOfferRepositoryAdapter;
import com.easylink.easylink.vibe_service.web.dto.OfferPatchRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements CreateOfferUseCase {

    private final ModelMapper modelMapper;
    private final VibeRepositoryPort vibeRepositoryPort;
    private final JpaOfferRepositoryAdapter jpaOfferRepositoryAdapter;
    private final AmplitudeService amplitudeService;
    private final OfferRateLimitPort rateLimitPort;

    private final JpaInteractionRepositoryAdapter interactionRepositoryAdapter;
    private final NotificationService notificationService;

    private static final int MONEY_MAX = 1_000_000;
    private static final int PERCENT_MAX = 100;
    private static final int INTERVAL_MAX_MIN = 10080; // 7 days

    @Override
    public OfferDto create(CreateOfferCommand createOfferCommand) {

        UUID vibeId = createOfferCommand.getVibeId();
        Vibe vibe = vibeRepositoryPort.findById(vibeId)
                .orElseThrow(() -> new IllegalArgumentException("Vibe not found"));

        if (!VibeType.BUSINESS.equals(vibe.getType())) {
            throw new IllegalArgumentException("Offer creation is allowed only for BUSINESS vibes");
        }

        String key = vibe.getId().toString();

        if (!rateLimitPort.canCreateOffer(key)) {
            throw new OfferLimitExceededException("Offer limit exceeded for user: " + vibe.getName());
        }

        Offer offer = modelMapper.map(createOfferCommand, Offer.class);
        offer.setVibe(vibe);
        offer.setActive(true);

        offer.setStartTime(createOfferCommand.getStartTime() != null
                ? createOfferCommand.getStartTime()
                : LocalDateTime.now());

        offer.setEndTime(createOfferCommand.getEndTime());

        Offer offerSaved = jpaOfferRepositoryAdapter.save(offer);
        rateLimitPort.incrementOffer(key);

        var subs = interactionRepositoryAdapter.findActiveSubscribersByTarget(vibe);
        var notified = new java.util.HashSet<String>();

        String title = "New offer";
        String body = vibe.getName() + " posted: " + offerSaved.getTitle();
        String link = "/view/" + vibe.getId();

        for (var s : subs) {
            String subscriberUserId = s.getSubscriberVibe().getVibeAccountId().toString();

            if (subscriberUserId.equals(vibe.getVibeAccountId().toString())) continue;
            if (!notified.add(subscriberUserId)) continue;

            notificationService.create(subscriberUserId, "OFFER", title, body, link);
        }

        amplitudeService.sendEvent(
                vibe.getName(),
                "Created Offer",
                Map.of(
                        "offerId", offerSaved.getId(),
                        "vibeId", vibe.getId(),
                        "title", offerSaved.getTitle(),
                        "source", "backend"
                )
        );

        OfferDto offerDto = modelMapper.map(offerSaved, OfferDto.class);
        offerDto.setVibeId(vibe.getId());
        return offerDto;
    }

    public List<OfferDto> findAllById(UUID id){
        Vibe vibe = vibeRepositoryPort.findById(id).orElseThrow(()->new IllegalArgumentException("Vibe not found"));

        List<Offer> offerList = jpaOfferRepositoryAdapter.findAllByVibe(vibe);

        List<OfferDto> offerDtoList = offerList.stream().map(offer -> modelMapper.map(offer,OfferDto.class)).toList();

        return offerDtoList;
    }

    public OfferDto findOfferById(UUID id){

        Offer offer =jpaOfferRepositoryAdapter.findById(id).orElseThrow(()->new RuntimeException("Offer not found!"));

        OfferDto offerDto = modelMapper.map(offer, OfferDto.class);

        return offerDto;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T getValue(Map<String, Object> fields, String key, Class<T> targetType) {
        Object value = fields.get(key);
        if (value == null) return null;

        if (targetType.isInstance(value)) return (T) value;

        if (targetType.isEnum()) {
            if (value instanceof String s) {
                return (T) Enum.valueOf((Class<? extends Enum>) targetType, s.trim().toUpperCase());
            }
        }

        if (targetType == Integer.class) {
            if (value instanceof Number) return (T) Integer.valueOf(((Number) value).intValue());
            if (value instanceof String) return (T) Integer.valueOf(Integer.parseInt((String) value));
        }

        if (targetType == Boolean.class) {
            if (value instanceof Boolean) return (T) value;
            if (value instanceof String) return (T) Boolean.valueOf(Boolean.parseBoolean((String) value));
        }

        if (targetType == LocalDateTime.class) {
            if (value instanceof String) return (T) LocalDateTime.parse((String) value);
        }

        if (targetType == String.class) return (T) value.toString();

        throw new IllegalArgumentException("Unsupported type or incompatible value: " + key + " → " + value);
    }



    public void updateOfferFields(UUID id, Map<String,Object> updatedFields, Jwt jwt) {
        Offer offer = jpaOfferRepositoryAdapter.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        UUID meAccount = extractAccountId(jwt);
        UUID ownerAccount = offer.getVibe() != null
                ? offer.getVibe().getVibeAccountId()
                : null;

        if (meAccount == null || ownerAccount == null || !ownerAccount.equals(meAccount)) {
            throw new AccessDeniedException("Not allowed");
        }
        
        if (updatedFields.containsKey("title")) {
            offer.setTitle(getValue(updatedFields, "title", String.class));
        }
        if (updatedFields.containsKey("description")) {
            offer.setDescription(getValue(updatedFields, "description", String.class));
        }
        if (updatedFields.containsKey("discountType")) {
            offer.setDiscountType(getValue(updatedFields, "discountType", DiscountType.class));
        }
        if (updatedFields.containsKey("initialDiscount")) {
            offer.setInitialDiscount(getValue(updatedFields, "initialDiscount", Integer.class));
        }
        if (updatedFields.containsKey("currentDiscount")) {
            offer.setCurrentDiscount(getValue(updatedFields, "currentDiscount", Integer.class));
        }
        if (updatedFields.containsKey("decreaseStep")) {
            offer.setDecreaseStep(getValue(updatedFields, "decreaseStep", Integer.class));
        }
        if (updatedFields.containsKey("decreaseIntervalMinutes")) {
            offer.setDecreaseIntervalMinutes(getValue(updatedFields, "decreaseIntervalMinutes", Integer.class));
        }
        if (updatedFields.containsKey("active")) {
            offer.setActive(getValue(updatedFields, "active", Boolean.class));
        }
        if (updatedFields.containsKey("startTime")) {
            offer.setStartTime(getValue(updatedFields, "startTime", LocalDateTime.class));
        }
        if (updatedFields.containsKey("endTime")) {
            offer.setEndTime(getValue(updatedFields, "endTime", LocalDateTime.class));
        }

        jpaOfferRepositoryAdapter.save(offer);
    }

    public void deleteOffer(UUID offerId, Jwt jwt) {
        Offer offer = jpaOfferRepositoryAdapter.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        UUID meAccount = extractAccountId(jwt);
        UUID ownerAccount = offer.getVibe() != null
                ? offer.getVibe().getVibeAccountId()
                : null;

        if (meAccount == null || ownerAccount == null || !ownerAccount.equals(meAccount)) {
            throw new AccessDeniedException("Not allowed");
        }

        jpaOfferRepositoryAdapter.delete(offer);

        // rate-limit key MUST be vibeId (как в create)
        String key = offer.getVibe() != null
                ? offer.getVibe().getId().toString()
                : null;

        if (key != null) {
            rateLimitPort.decrementOffer(key);
        }
    }


    private UUID extractAccountId(Jwt jwt) {
        if (jwt == null) return null;

        // 1) standard JWT subject (account/user id)
        String sub = jwt.getSubject();
        if (sub != null) {
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // 2) common custom claims (fallbacks)
        Object v = jwt.getClaims().get("accountId");
        if (v == null) v = jwt.getClaims().get("userId");
        if (v == null) v = jwt.getClaims().get("vibeAccountId");
        if (v == null) v = jwt.getClaims().get("vibe_account_id");

        if (v == null) return null;

        try {
            return UUID.fromString(String.valueOf(v));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public void updateOffer(UUID id, OfferPatchRequest p, Jwt jwt) {
        Offer offer = jpaOfferRepositoryAdapter.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        UUID meAccount = extractAccountId(jwt);
        UUID ownerAccount = offer.getVibe() != null ? offer.getVibe().getVibeAccountId() : null;

        if (meAccount == null || ownerAccount == null || !ownerAccount.equals(meAccount)) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed");
        }

        if (p.getTitle() != null) {
            offer.setTitle(p.getTitle().trim());
        }
        if (p.getDescription() != null) {
            offer.setDescription(p.getDescription().trim());
        }
        if (p.getDiscountType() != null) {
            offer.setDiscountType(p.getDiscountType());
        }
        if (p.getInitialDiscount() != null) {
            offer.setInitialDiscount(p.getInitialDiscount());
        }
        if (p.getCurrentDiscount() != null) {
            offer.setCurrentDiscount(p.getCurrentDiscount());
        }
        if (p.getDecreaseStep() != null) {
            offer.setDecreaseStep(p.getDecreaseStep());
        }
        if (p.getDecreaseIntervalMinutes() != null) {
            offer.setDecreaseIntervalMinutes(p.getDecreaseIntervalMinutes());
        }
        if (p.getActive() != null) {
            offer.setActive(p.getActive());
        }
        if (p.getStartTime() != null) {
            offer.setStartTime(p.getStartTime());
        }
        if (p.getEndTime() != null) {
            offer.setEndTime(p.getEndTime());
        }


        validateTimeRange(offer.getStartTime(), offer.getEndTime());
        validateDiscounts(offer);

        jpaOfferRepositoryAdapter.save(offer);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    private void validateDiscounts(Offer o) {
        if (o.getDiscountType() == null) {
            throw new IllegalArgumentException("discountType is required");
        }

        int init = o.getInitialDiscount();
        int cur = o.getCurrentDiscount();
        int step = o.getDecreaseStep();
        int interval = o.getDecreaseIntervalMinutes();

        if (init < 0 || cur < 0 || step < 0 || interval < 0) {
            throw new IllegalArgumentException("Discount fields must be >= 0");
        }

        switch (o.getDiscountType()) {
            case PERCENTAGE -> {
                if (init > PERCENT_MAX || cur > PERCENT_MAX) {
                    throw new IllegalArgumentException("For PERCENTAGE, discounts must be 0..100");
                }
                if (step != 0 || interval != 0) {
                    throw new IllegalArgumentException("For PERCENTAGE, decreaseStep and decreaseIntervalMinutes must be 0");
                }
            }

            case FIXED -> {
                if (init > MONEY_MAX || cur > MONEY_MAX) {
                    throw new IllegalArgumentException("For FIXED, discounts are too large");
                }
                if (step != 0 || interval != 0) {
                    throw new IllegalArgumentException("For FIXED, decreaseStep and decreaseIntervalMinutes must be 0");
                }
            }

            case DYNAMIC -> {
                if (init > PERCENT_MAX || cur > PERCENT_MAX) {
                    throw new IllegalArgumentException("For DYNAMIC, discounts must be 0..100");
                }
                if (step <= 0) {
                    throw new IllegalArgumentException("For DYNAMIC, decreaseStep must be > 0");
                }
                if (step > PERCENT_MAX) {
                    throw new IllegalArgumentException("For DYNAMIC, decreaseStep must be <= 100");
                }
                if (interval <= 0 || interval > INTERVAL_MAX_MIN) {
                    throw new IllegalArgumentException("For DYNAMIC, decreaseIntervalMinutes must be 1..10080");
                }
                if (cur > init) {
                    throw new IllegalArgumentException("For DYNAMIC, currentDiscount must be <= initialDiscount");
                }
            }

            case COUPON, PERSONALIZED -> {
                if (init != 0 || cur != 0 || step != 0 || interval != 0) {
                    throw new IllegalArgumentException("For COUPON/PERSONALIZED, discount fields must be 0 (computed elsewhere)");
                }
            }

            case TIME_BASED -> {
                if (init > PERCENT_MAX || cur > PERCENT_MAX) {
                    throw new IllegalArgumentException("For TIME_BASED, discounts must be 0..100");
                }
                if (step != 0 || interval != 0) {
                    throw new IllegalArgumentException("For TIME_BASED, decreaseStep and decreaseIntervalMinutes must be 0");
                }
            }
        }
    }
}
