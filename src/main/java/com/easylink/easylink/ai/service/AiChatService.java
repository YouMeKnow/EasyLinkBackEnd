package com.easylink.easylink.ai.service;

import com.easylink.easylink.ai.dto.AiChatRequest;
import com.easylink.easylink.ai.dto.AiChatResponse;
import com.easylink.easylink.ai.dto.IntentResult;
import com.easylink.easylink.ai.provider.AiProvider;
import com.google.genai.Client;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final AiProvider aiProvider;
    private final IntentService intentService;
    private final IntentRouter intentRouter;

    public AiChatService(AiProvider aiProvider,IntentService intentService,IntentRouter intentRouter){
        this.aiProvider=aiProvider;
        this.intentService=intentService;
        this.intentRouter=intentRouter;
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }

        IntentResult intentResult = intentService.detectIntent(request.message());

        return intentRouter.route(intentResult, request.message());
    }
}
