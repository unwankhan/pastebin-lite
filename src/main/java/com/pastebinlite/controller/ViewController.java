package com.pastebinlite.controller;

import com.pastebinlite.model.Paste;
import com.pastebinlite.repository.PasteRepository;
import com.pastebinlite.service.PasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
public class ViewController {

    @Autowired
    private PasteService pasteService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/create")
    public String create() {
        return "create";
    }

    @GetMapping("/p/{id}")
    public String viewPasteHtml(
            @PathVariable String id,
            @RequestHeader(value = "x-test-now-ms", required = false) Long testNowMs,
            Model model) {

        // Single database fetch
        Optional<Paste> pasteOpt = pasteService.findPasteByPasteId(id);

        if (pasteOpt.isEmpty()) {
            model.addAttribute("errorType", "not_found");
            model.addAttribute("errorTitle", "Paste Not Found");
            model.addAttribute("errorMessage", "The paste you're looking for doesn't exist or has been deleted.");
            return "error";
        }

        Paste paste = pasteOpt.get();
        Instant now = getNowInstant(testNowMs);

        // Check constraints
        if (paste.isExpired(now)) {
            model.addAttribute("errorType", "expired");
            model.addAttribute("errorTitle", "Paste Expired");
            model.addAttribute("errorMessage", "This paste has expired and is no longer available.");
            if (paste.getExpiresAt() != null) {
                model.addAttribute("errorDetails",
                        "Expired on: " + DateTimeFormatter.ISO_INSTANT.format(paste.getExpiresAt().toInstant()));
            }
            return "error";
        }

        if (paste.isViewLimitExceeded()) {
            model.addAttribute("errorType", "limit_reached");
            model.addAttribute("errorTitle", "View Limit Reached");
            model.addAttribute("errorMessage",
                    "This paste has been viewed " + paste.getViewCount() +
                            " times and reached its maximum limit of " + paste.getMaxViews() + " views.");
            return "error";
        }

        // All constraints passed, show the paste
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        model.addAttribute("pasteId", paste.getPasteId());
        model.addAttribute("content", paste.getContent());
        model.addAttribute("createdAt", formatter.format(paste.getCreatedAt()));
        model.addAttribute("viewCount", paste.getViewCount());
        model.addAttribute("maxViews", paste.getMaxViews());
        model.addAttribute("remainingViews", paste.getRemainingViews());

        // Handle expiration display
        if (paste.getExpiresAt() != null) {
            model.addAttribute("expiresAt", formatter.format(paste.getExpiresAt().toInstant()));
            long secondsRemaining = paste.getExpiresAt().getSeconds() - now.getEpochSecond();
            model.addAttribute("timeRemaining",
                    secondsRemaining <= 0 ? "Expired" : formatTimeRemaining(secondsRemaining));
        } else {
            model.addAttribute("expiresAt", "Never");
            model.addAttribute("timeRemaining", "Never");
        }

        // Set status (we already know it's active since constraints passed)
        model.addAttribute("status", "active");

        return "view";
    }

    @GetMapping("/error")
    public String errorPage(@RequestParam(value = "message", required = false) String message, Model model) {
        model.addAttribute("errorMessage", message != null ? message : "An error occurred");
        return "error";
    }

    private Instant getNowInstant(Long testNowMs) {
        if (testNowMs != null && testNowMs > 0) {
            return Instant.ofEpochMilli(testNowMs);
        }
        return Instant.now();
    }

    private String formatTimeRemaining(long seconds) {
        if (seconds < 60) return seconds + " seconds";
        if (seconds < 3600) return (seconds / 60) + " minutes";
        if (seconds < 86400) return (seconds / 3600) + " hours";
        return (seconds / 86400) + " days";
    }



}