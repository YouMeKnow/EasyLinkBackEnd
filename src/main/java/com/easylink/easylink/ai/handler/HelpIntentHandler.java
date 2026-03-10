package com.easylink.easylink.ai.handler;

import com.easylink.easylink.ai.dto.AiChatResponse;
import com.easylink.easylink.ai.dto.IntentResult;
import com.easylink.easylink.ai.service.GeminiService;
import com.easylink.easylink.enums.IntentType;

public class HelpIntentHandler implements IntentHandler{

    private final GeminiService geminiService;

    public HelpIntentHandler(GeminiService geminiService){
        this.geminiService=geminiService;
    }

    @Override
    public IntentType intent() {
        return IntentType.HELP;
    }

    @Override
    public AiChatResponse handle(IntentResult intentResult, String userMessage) {

        String reply = geminiService.generateReply(userMessage);

        return new AiChatResponse(reply);
    }
}
