package com.example.blog_rag.controller;

import com.example.blog_rag.service.CrawlService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private final CrawlService crawlService;

    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @PostMapping
    public List<String> crawl() throws Exception {
        return crawlService.crawlAndStore();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("count", crawlService.getPostingCount());   // vector_store 테이블 row 수 반환
    }
}
