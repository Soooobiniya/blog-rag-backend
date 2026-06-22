package com.example.blog_rag.service;

import com.example.blog_rag.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public ChatResponse chat(String message) {
        // 1. 질문과 유사한 문서 검색
        List<org.springframework.ai.document.Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(message)
                        .topK(3)
                        .build()
        );

        // 2. 검색된 문서로 컨텍스트 구성
        String context = docs.stream()
                .map(org.springframework.ai.document.Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);

        // 3. 출처 URL 추출
        List<String> sources = docs.stream()
                .map(doc -> (String) doc.getMetadata().get("url"))
                .filter(Objects::nonNull)
                .toList();

        // 4. LLM에 질문
        String prompt = """
                당신은 블로그 글을 기반으로 답변하는 AI 비서입니다.
                반드시 한국어로만 답변하세요.
                아래 블로그 글들을 참고해서 질문에 답해줘.
                블로그 글에 있는 내용만 답하고, 추론하거나 없는 내용은 절대 추가하지 마세요.
                없는 내용은 "블로그에서 찾을 수 없어요" 라고 답해줘.
                
                [참고 블로그 글]
                %s
                
                [질문]
                %s
                """.formatted(context, message);

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new ChatResponse(answer, sources);
    }
}