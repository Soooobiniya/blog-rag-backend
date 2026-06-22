package com.example.blog_rag.controller;

import com.example.blog_rag.dto.ChatRequest;
import com.example.blog_rag.dto.ChatResponse;
import com.example.blog_rag.service.RagService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return ragService.chat(request.message());
    }
}