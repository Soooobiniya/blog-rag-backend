package com.example.blog_rag.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CrawlService {

    @Value("${naver.blog-id}")
    private String blogId;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public CrawlService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public int getPostingCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store", Integer.class);
        return count != null ? count : 0;
    }

    public List<String> crawlAndStore() throws Exception {
        List<String> savedTitles = new ArrayList<>();
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();

        for (int page = 1; page <= 5; page++) {
            String listUrl = "https://blog.naver.com/PostList.naver?blogId=" + blogId + "&currentPage=" + page;
            Document listPage = Jsoup.connect(listUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Elements posts = listPage.select("div[id^=post-view]");
            if (posts.isEmpty()) break;

            for (Element post : posts) {
                String postId = post.attr("id").replace("post-view", "");
                String postUrl = "https://blog.naver.com/" + blogId + "/" + postId;
                String title = postId;

                String content = crawlContent(postUrl);
                if (content.isEmpty()) continue;

                org.springframework.ai.document.Document doc =
                        new org.springframework.ai.document.Document(content, Map.of(
                                "title", title,
                                "url", postUrl
                        ));
                documents.add(doc);
                savedTitles.add(postUrl);
            }
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        return savedTitles;
    }

    private String crawlContent(String url) {
        try {
            // iframe 내부 URL로 변환
            String logNo = url.substring(url.lastIndexOf("/") + 1);
            String realUrl = "https://blog.naver.com/PostView.naver?blogId=" + blogId + "&logNo=" + logNo;

            Document doc = Jsoup.connect(realUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            String content = doc.select("div.se-main-container").text();
            if (content.isEmpty()) {
                content = doc.select("div#postViewArea").text();
            }
            if (content.isEmpty()) {
                content = doc.select("div.post-view").text();
            }
            return content;
        } catch (Exception e) {
            return "";
        }
    }
}