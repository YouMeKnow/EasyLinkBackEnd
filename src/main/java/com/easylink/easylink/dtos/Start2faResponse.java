package com.easylink.easylink.dtos;

import java.util.UUID;

public class Start2faResponse {

    private boolean requires2fa;
    private UUID challengeId;

    public Start2faResponse() {}

    public Start2faResponse(boolean requires2fa, UUID challengeId) {
        this.requires2fa = requires2fa;
        this.challengeId = challengeId;
    }

    public boolean isRequires2fa() {
        return requires2fa;
    }

    public void setRequires2fa(boolean requires2fa) {
        this.requires2fa = requires2fa;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }
}