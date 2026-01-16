package com.easylink.easylink.dtos;

import java.util.UUID;

public class Verify2faDTO {

    private UUID challengeId;
    private String code;
    private boolean rememberMe;

    public Verify2faDTO() {}

    public UUID getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(UUID challengeId) {
        this.challengeId = challengeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}