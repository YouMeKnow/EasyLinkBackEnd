package com.easylink.easylink.ai.service;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final String model;

    public GeminiService(@Value("${gemini.model}") String model){
        this.model = model;
    }

    public String generateReply(String userMessage){
        Client client = new Client();

        GenerateContentResponse response = client.models.generateContent(
                model,
                userMessage,
                null
        );

        return response.text();

    }
}
