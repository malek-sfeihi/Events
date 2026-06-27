package com.eventmanagment.backend.chat.dto;

import java.util.List;

public record ChatRequest(String message, List<HistoryItem> history) {

    public record HistoryItem(String role, String content) {}
}