package com.pastebinlite.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pastebinlite.model.Paste;
import com.pastebinlite.service.PasteService;
import com.pastebinlite.repository.PasteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class PasteController {

    @Autowired
    private PasteService pasteService;

    @Autowired
    private PasteRepository pasteRepository;

    // Health check endpoint that also checks persistence reachability
    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        boolean ok = true;
        try {
            // cheap persistence call
            long count = pasteRepository.count();
            response.put("db_count", count);
        } catch (Exception ex) {
            ok = false;
            response.put("db_error", ex.getMessage());
        }
        response.put("ok", ok);
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    // Create paste endpoint
    @PostMapping("/pastes")
    public ResponseEntity<?> createPaste(@RequestBody CreatePasteRequest request) {
        // Input validation
        if (request.content == null || request.content.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Content is required and must be non-empty"));
        }

        if (request.ttlSeconds != null && request.ttlSeconds < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ttl_seconds must be ≥ 1 if present"));
        }

        if (request.maxViews != null && request.maxViews < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "max_views must be ≥ 1 if present"));
        }

        Paste paste = pasteService.createPaste(
                request.content,
                request.ttlSeconds,
                request.maxViews
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", paste.getPasteId());
        response.put("url", pasteService.getPasteUrl(paste.getPasteId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get paste endpoint (API) - returns JSON and counts as a view
    @GetMapping("/pastes/{id}")
    public ResponseEntity<?> getPasteApi(
            @PathVariable("id") String id,
            @RequestHeader(value = "x-test-now-ms", required = false) Long testNowMs) {

        Optional<Paste> pasteOpt = pasteService.getPaste(id, testNowMs);

        if (pasteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Paste not found or unavailable"));
        }

        Paste paste = pasteOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("content", paste.getContent());
        response.put("remaining_views", paste.getRemainingViews()); // null if unlimited
        response.put("expires_at", paste.getExpiresAtISO()); // ISO string or null
        response.put("created_at", paste.getCreatedAt().toString());
        response.put("max_views", paste.getMaxViews());
        response.put("view_count", paste.getViewCount());

        return ResponseEntity.ok(response);
    }

    // DTO for create request with JSON property mappings (snake_case)
    static class CreatePasteRequest {
        @JsonProperty("content")
        public String content;

        @JsonProperty("ttl_seconds")
        public Integer ttlSeconds;

        @JsonProperty("max_views")
        public Integer maxViews;

        public CreatePasteRequest() {}

        public String getAllContent() {
            return content + " " + ttlSeconds + " " + maxViews;
        }
    }
}

