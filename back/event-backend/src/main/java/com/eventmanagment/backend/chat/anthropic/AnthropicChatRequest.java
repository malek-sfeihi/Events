package com.eventmanagment.backend.chat.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Payload compatible OpenAI — utilisé pour Groq et autres LLM compatibles. */
public record AnthropicChatRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        List<AnthropicChatMessage> messages) {

    public record AnthropicChatMessage(String role, String content) {}
}