package com.example.blog_rag.dto;

import java.util.List;

public record ChatResponse(String answer, List<String> sources) {}