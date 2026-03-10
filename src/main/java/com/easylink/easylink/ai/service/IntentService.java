package com.easylink.easylink.ai.service;

import com.easylink.easylink.ai.dto.IntentResult;
import com.easylink.easylink.enums.IntentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class IntentService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public IntentService(GeminiService geminiService, ObjectMapper objectMapper){
        this.geminiService=geminiService;
        this.objectMapper=objectMapper;
    }

    public IntentResult detectIntent(String userMessage) {
        try {
            String rawJson = geminiService.detectIntentRaw(userMessage);

            System.out.println("RAW GEMINI RESPONSE: " + rawJson);

            String cleanedJson = cleanJson(rawJson);
            System.out.println("CLEANED JSON: " + cleanedJson);

            JsonNode root = objectMapper.readTree(rawJson);

            String intentValue = root.path("intent").asText("UNKNOWN");
            String category = root.path("category").asText("");

            IntentType intentType;
            try {
                intentType = IntentType.valueOf(intentValue);
            } catch (IllegalArgumentException e) {
                intentType = IntentType.UNKNOWN;
            }

            return new IntentResult(intentType, category);

        }catch (Exception e){
            return new IntentResult(IntentType.UNKNOWN,"");
        }
    }

    private String cleanJson(String raw) {
        if (raw == null) {
            return "";
        }

        String cleaned = raw.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }

}
