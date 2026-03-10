package com.easylink.easylink.ai.service;
import com.easylink.easylink.ai.provider.AiProvider;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService implements AiProvider {

    private final String model;
    private final Client client;

    public GeminiService(Client client,@Value("${gemini.model}") String model){
        this.model = model;
        this.client=client;
    }

    public String generateReply(String userMessage){
        return callGeminiForChat(userMessage);
    }

    public String detectIntentRaw(String userMessage){
        String prompt = """
                Analyze the user message and return JSON only.
                
                Available intents:
                -FIND_BUSINESS
                -SHOW_OFFERS
                -CREATE_VIBE
                -HELP
                -UNKNOWN
                
                Rules:
                -If the user wants to find a business, place, restaurant, coffe shop, pizza, service, or anything nearby, use FIND_BUSINESS.
                -If the user wants to discount, deals, or promotions, use SHOW_OFFERS.
                -If the user wants to create a vibe/profile/page, use CREATE_VIBE.
                -If the user asks how the app works or asks for help, use HELP
                -category should contain one short word if possible, otherwise empty string.
                
                Return ONLY valid JSON.
                Do not include markdown.
                Do not include explanations.
                
                Example:
                {
                 "intent": "FIND_BUSINESS",
                 "category": "pizza"
                }
                
                User message:
                "%s"
                
                """.formatted(userMessage);

        return callGeminiForIntent(prompt);
    }

    @Retry(name = "geminiRetry", fallbackMethod = "intentFallback")
    @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "intentFallback")
    public String callGeminiForIntent(String prompt) {
        GenerateContentResponse response = client.models.generateContent(
                model,
                prompt,
                null
        );

        return response.text();
    }

    @Retry(name = "geminiRetry", fallbackMethod = "chatFallback")
    @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "chatFallback")
    public String callGeminiForChat(String userMessage) {
        GenerateContentResponse response = client.models.generateContent(
                model,
                userMessage,
                null
        );

        return response.text();
    }


    public String intentFallBack(String prompt, Exception e){
        return """
                {
                    "intent": "UNKNOWN",
                    "category": ""
                }
                """;
    }

    public String chatFallBack(String userMessage, Exception e){
        return "AI service is temporary unavailable. Please try again later.";
    }
}
