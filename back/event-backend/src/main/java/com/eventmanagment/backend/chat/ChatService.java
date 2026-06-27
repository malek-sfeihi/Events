package com.eventmanagment.backend.chat;

import com.eventmanagment.backend.chat.anthropic.AnthropicChatRequest;
import com.eventmanagment.backend.chat.anthropic.AnthropicChatRequest.AnthropicChatMessage;
import com.eventmanagment.backend.chat.anthropic.AnthropicChatResponse;
import com.eventmanagment.backend.chat.dto.ChatRequest;
import com.eventmanagment.backend.chat.dto.ChatResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RestClient.Builder restClientBuilder;

    @Value("${groq.api-key:}")
    private String apiKey;

    private static final String GROQ_BASE_URL = "https://api.groq.com";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final String SYSTEM_PROMPT = """
            Tu es EventBot, l'assistant intelligent d'EventSpace, une plateforme tunisienne de gestion d'événements.
            Tu aides les organisateurs à planifier leurs événements, choisir des prestataires, gérer les budgets,
            rédiger des invitations et répondre à toutes leurs questions liées à l'organisation événementielle.
            Réponds toujours en français, de manière concise, structurée et professionnelle.
            """;

    public ChatResponse chat(String userEmail, ChatRequest request) {
        List<AnthropicChatMessage> messages = buildMessages(request);

        AnthropicChatRequest body = new AnthropicChatRequest(MODEL, 1024, messages);

        AnthropicChatResponse response = restClientBuilder
                .baseUrl(GROQ_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build()
                .post()
                .uri("/openai/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicChatResponse.class);

        String reply = (response != null
                        && response.choices() != null
                        && !response.choices().isEmpty()
                        && response.choices().get(0).message() != null)
                ? response.choices().get(0).message().content()
                : "Désolé, je n'ai pas pu générer une réponse.";

        ChatMessage entity = new ChatMessage();
        entity.setUserEmail(userEmail);
        entity.setUserMessage(request.message());
        entity.setAssistantReply(reply);
        entity.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(entity);

        return new ChatResponse(reply);
    }

    private List<AnthropicChatMessage> buildMessages(ChatRequest request) {
        List<AnthropicChatMessage> messages = new ArrayList<>();
        messages.add(new AnthropicChatMessage("system", SYSTEM_PROMPT));
        if (request.history() != null) {
            for (ChatRequest.HistoryItem h : request.history()) {
                messages.add(new AnthropicChatMessage(h.role(), h.content()));
            }
        }
        messages.add(new AnthropicChatMessage("user", request.message()));
        return messages;
    }
}