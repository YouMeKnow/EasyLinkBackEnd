package com.easylink.easylink.ai.service;

import com.easylink.easylink.ai.dto.AiChatResponse;
import com.easylink.easylink.ai.dto.IntentResult;
import com.easylink.easylink.ai.handler.IntentHandler;
import com.easylink.easylink.enums.IntentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import java.util.HashMap;

@Component
public class IntentRouter {

    private final Map<IntentType, IntentHandler> handlerMap = new HashMap<>();

    public IntentRouter(List<IntentHandler> handlers){
        for (IntentHandler intentHandler:handlers) {
            handlerMap.put(intentHandler.intent(),intentHandler);
        }
    }

    public AiChatResponse route(IntentResult intentResult,String message){

        IntentHandler handler = handlerMap.get(intentResult.intent());

        if (handler==null){
            return new AiChatResponse("Sorry, I don't understand");
        }

        return handler.handle(intentResult,message);
    }
}
