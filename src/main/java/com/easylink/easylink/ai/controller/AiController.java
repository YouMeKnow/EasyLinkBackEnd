package com.easylink.easylink.ai.controller;

import com.easylink.easylink.ai.dto.AiChatRequest;
import com.easylink.easylink.ai.dto.AiChatResponse;
import com.easylink.easylink.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public AiChatResponse chat(@RequestBody AiChatRequest request){
        return aiChatService.chat(request);
    }
}
