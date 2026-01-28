package com.devsecwatch.backend.service;

import com.devsecwatch.backend.dto.chat.ChatRequest;
import com.devsecwatch.backend.dto.chat.ChatResponse;
import com.devsecwatch.backend.model.User;

public interface ChatService {
    ChatResponse processMessage(ChatRequest request, User user);
}
