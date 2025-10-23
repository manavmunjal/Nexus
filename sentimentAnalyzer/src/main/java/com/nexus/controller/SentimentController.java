package com.nexus.controller;

import com.nexus.sentiment.SentimentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {
    private final SentimentService sentimentService;

    public SentimentController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    @GetMapping("/score")
    public double score(@RequestParam("text") String text) {
        try {
            return sentimentService.scoreFromText(text);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
