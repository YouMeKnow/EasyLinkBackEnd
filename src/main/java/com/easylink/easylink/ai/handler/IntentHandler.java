package com.easylink.easylink.ai.handler;

import com.easylink.easylink.ai.dto.AiChatResponse;
import com.easylink.easylink.ai.dto.IntentResult;
import com.easylink.easylink.enums.IntentType;

public interface IntentHandler {

    IntentType intent();

    AiChatResponse handle(IntentResult intentResult,String userMessage);

}
