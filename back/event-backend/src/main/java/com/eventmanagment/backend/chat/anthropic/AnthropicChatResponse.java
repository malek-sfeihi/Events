package com.eventmanagment.backend.chat.anthropic;

import java.util.List;

/** Réponse compatible OpenAI — utilisée pour Groq et autres LLM compatibles. */
public record AnthropicChatResponse(List<Choice> choices) {

    public record Choice(Message message) {}

    public record Message(String role, String content) {}
}