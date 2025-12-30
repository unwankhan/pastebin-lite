package com.pastebinlite.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pastebinlite.model.Paste;
import com.pastebinlite.service.PasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class PasteController {

    @Autowired
    private PasteService pasteService;

    @Autowired
    private MongoTemplate mongoTemplate;

    // Health check endpoint
    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            response.put("ok", true);
        } catch (Exception e) {
            response.put("ok", false);
        }
        return ResponseEntity.ok(response);
    }

    // Create paste endpoint
    @PostMapping("/pastes")
    public ResponseEntity<?> createPaste(@RequestBody CreatePasteRequest request) {
        //System.out.println("Creating Paste " + request.getAllContent());
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

        // Create paste
        Paste paste = pasteService.createPaste(
                request.content,
                request.ttlSeconds,
                request.maxViews
        );

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("id", paste.getPasteId());
        response.put("url", pasteService.getPasteUrl(paste.getPasteId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get paste endpoint (API) - returns JSON
    @GetMapping("/pastes/{id}")
    public ResponseEntity<?> getPasteApi(
            @PathVariable String id,
            @RequestHeader(value = "x-test-now-ms", required = false) Long testNowMs) {

        // This API should increment the view count (counts as a view)
        Optional<Paste> pasteOpt = pasteService.getPaste(id, testNowMs);

        if (pasteOpt.isEmpty()) {
            // Try to get the paste without incrementing to check why it's unavailable
            Optional<Paste> pasteForCheck = pasteService.getPasteForViewOnly(id, testNowMs);

            if (pasteForCheck.isEmpty()) {
                // Paste doesn't exist at all
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Paste not found"));
            }

            Paste paste = pasteForCheck.get();
            Instant now = pasteService.getNowInstant(testNowMs);

            if (paste.getExpiresAt() != null && now.isAfter(paste.getExpiresAt().toInstant())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Paste expired"));
            }

            if (paste.getMaxViews() != null && paste.getViewCount() >= paste.getMaxViews()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Paste view limit reached"));
            }

            // Generic error if none of the above
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Paste unavailable"));
        }

        Paste paste = pasteOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("content", paste.getContent());
        response.put("remaining_views", paste.getRemainingViews());
        response.put("expires_at", paste.getExpiresAtISO());

        return ResponseEntity.ok(response);
    }


    // Request DTO
    static class CreatePasteRequest {
        public String content;

        @JsonProperty("ttl_seconds")
        public Integer ttlSeconds;

        @JsonProperty("max_views")
        public Integer maxViews;

        public CreatePasteRequest() {}
    }
}
