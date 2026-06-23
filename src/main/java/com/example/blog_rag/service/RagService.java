package com.example.blog_rag.service;

import com.example.blog_rag.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Map;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public ChatResponse chat(String message, List<Map<String, String>> history) {
        String searchQuery = message;
        if (history != null && !history.isEmpty()) {
            String lastUserMsg = history.stream()
                    .filter(h -> h.get("role").equals("user"))
                    .reduce((first, second) -> second)
                    .map(h -> h.get("content"))
                    .orElse("");
            searchQuery = lastUserMsg + " " + message;
        }

        List<org.springframework.ai.document.Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchQuery)
                        .topK(5)
                        .similarityThreshold(0.1)
                        .build()
        );

        String context = docs.stream()
                .map(org.springframework.ai.document.Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);

        List<String> sources = docs.stream()
                .map(doc -> (String) doc.getMetadata().get("url"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 대화 히스토리 조합
        StringBuilder historyText = new StringBuilder();
        if (history != null) {
            for (Map<String, String> h : history) {
                String role = h.get("role").equals("user") ? "사용자" : "AI";
                historyText.append(role).append(": ").append(h.get("content")).append("\n");
            }
        }

        String prompt = """
        You are a Korean AI assistant. You MUST respond in Korean only. Never use Chinese, Japanese, or any other language.
        
        아래 블로그 글들을 참고해서 질문에 답해줘.
        블로그 글에 있는 내용만 답하고, 추론하거나 없는 내용은 절대 추가하지 마세요.
        없는 내용은 "블로그에서 찾을 수 없어요" 라고 답해줘.
        반드시 한국어로만 답변하세요.
        
        [이전 대화 - 반드시 참고하여 문맥을 이어서 답변하세요]
        %s
        
        [참고 블로그 글]
        %s
        
        [현재 질문 - 이전 대화 문맥을 반드시 고려하세요]
        %s
        """.formatted(historyText, context, message);

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new ChatResponse(answer, sources);
    }
}