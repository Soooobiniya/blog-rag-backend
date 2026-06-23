package com.example.blog_rag.dto;

import java.util.List;
import java.util.Map;

public record ChatRequest(String message, List<Map<String, String>> history) {}
