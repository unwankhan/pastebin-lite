package com.pastebinlite.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pastebinlite.model.Paste;
import com.pastebinlite.model.PasteResult;
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

        PasteResult result = pasteService.getPaste(id, testNowMs);  // Call updated method.

        if (result.isError()) {
            String errorMsg = result.getErrorMessage();
            Map<String, Object> errorBody = Map.of("error", errorMsg);  // Simple.

            if (errorMsg.contains("expired")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            } else if (errorMsg.contains("limit reached")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", errorMsg, "details", "View count exceeded maximum allowed."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            }
        }

        Paste paste = result.getPaste();
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
