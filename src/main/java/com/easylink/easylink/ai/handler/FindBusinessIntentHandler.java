package com.easylink.easylink.ai.handler;

import com.easylink.easylink.ai.dto.AiChatResponse;
import com.easylink.easylink.ai.dto.IntentResult;
import com.easylink.easylink.enums.IntentType;

public class FindBusinessIntentHandler implements IntentHandler{

    @Override
    public IntentType intent() {
        return IntentType.FIND_BUSINESS;
    }

    @Override
    public AiChatResponse handle(IntentResult intentResult, String userMessage) {

        String category = intentResult.category();

        return new AiChatResponse("Searching for category: "+category);
    }
}
