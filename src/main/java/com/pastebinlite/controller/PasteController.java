package com.pastebinlite.controller;

import com.pastebinlite.model.Paste;
import com.pastebinlite.service.PasteService;
import org.springframework.beans.factory.annotation.Autowired;
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

    // Health check endpoint
    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("timestamp", Instant.now().toString());
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

        Optional<Paste> pasteOpt = pasteService.getPasteForViewOnly(id, testNowMs);

        if (pasteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Paste not found or unavailable"));
        }

        Paste paste = pasteOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("content", paste.getContent());
        response.put("remaining_views", paste.getRemainingViews());
        response.put("expires_at", paste.getExpiresAt() != null ?
                paste.getExpiresAt().toString() : null);
        response.put("created_at", paste.getCreatedAt().toString());
        response.put("max_views", paste.getMaxViews());
        response.put("view_count", paste.getViewCount());

        return ResponseEntity.ok(response);
    }

    @Controller
    public class ViewController {

        @Autowired
        private PasteService pasteService;

        // Home page - simple redirect to template
        @GetMapping("/")
        public String home() {
            return "index";  // This will look for src/main/resources/templates/index.html
        }

        // Create page - simple redirect to template
        @GetMapping("/create")
        public String create() {
            return "create";  // This will look for src/main/resources/templates/create.html
        }

        // View paste - This needs to process the paste data
        @GetMapping("/p/{id}")
        public String viewPasteHtml(
                @PathVariable String id,
                @RequestHeader(value = "x-test-now-ms", required = false) Long testNowMs,
                Model model) {

            Optional<Paste> pasteOpt = pasteService.getPaste(id, testNowMs);
            //System.out.println("output result "+pasteOpt.isPresent());
            if (pasteOpt.isEmpty()) {
                //System.out.println("Paste not found or unavailable");
                model.addAttribute("errorMessage", "This paste is not available. It may have expired, been deleted, or reached its view limit.");
                return "error";
            }

            Paste paste = pasteOpt.get();

            // Format dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

            // Calculate if expired
            Instant now = testNowMs != null ? Instant.ofEpochMilli(testNowMs) : Instant.now();
            boolean isExpired = paste.getExpiresAt() != null && now.isAfter(paste.getExpiresAt().toInstant());
            boolean isLimitReached = paste.getMaxViews() != null && paste.getViewCount() >= paste.getMaxViews();

            // Add attributes to model
            model.addAttribute("pasteId", paste.getPasteId());
            model.addAttribute("content", paste.getContent());
            model.addAttribute("createdAt", formatter.format(paste.getCreatedAt()));
            model.addAttribute("viewCount", paste.getViewCount());
            model.addAttribute("maxViews", paste.getMaxViews());
            model.addAttribute("remainingViews", paste.getRemainingViews());

            if (paste.getExpiresAt() != null) {
                model.addAttribute("expiresAt", formatter.format(paste.getExpiresAt().toInstant()));

                // Calculate time remaining
                long secondsRemaining = paste.getExpiresAt().getSeconds() - now.getEpochSecond();
                if (secondsRemaining <= 0) {
                    model.addAttribute("timeRemaining", "Expired");
                } else {
                    model.addAttribute("timeRemaining", formatTimeRemaining(secondsRemaining));
                }
            } else {
                model.addAttribute("expiresAt", "Never");
                model.addAttribute("timeRemaining", "Never");
            }

            // Determine status
            if (isExpired) {
                model.addAttribute("status", "expired");
            } else if (isLimitReached) {
                model.addAttribute("status", "limit_reached");
            } else {
                model.addAttribute("status", "active");
            }

            return "view";
        }

        // Error page
        @GetMapping("/error")
        public String errorPage(@RequestParam(value = "message", required = false) String message, Model model) {
            model.addAttribute("errorMessage", message != null ? message : "An error occurred");
            return "error";
        }

        private String formatTimeRemaining(long seconds) {
            if (seconds < 60) return seconds + " seconds";
            if (seconds < 3600) return (seconds / 60) + " minutes";
            if (seconds < 86400) return (seconds / 3600) + " hours";
            return (seconds / 86400) + " days";
        }
    }

    // Request DTO
    static class CreatePasteRequest {
        public String content;
        public Integer ttlSeconds;
        public Integer maxViews;

        public CreatePasteRequest() {}

        public String getAllContent() {
            return content+ " "+ ttlSeconds+ " "+ maxViews;
        }
    }
}
