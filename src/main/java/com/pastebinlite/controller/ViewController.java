package com.pastebinlite.controller;

import com.pastebinlite.model.Paste;
import com.pastebinlite.model.PasteResult;
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

        // Single service call with result handling
        PasteResult result = pasteService.getPasteForViewOnly(id, testNowMs);

        if (result.isError()) {
            String errorMsg = result.getErrorMessage();
            if (errorMsg.contains("not found")) {
                model.addAttribute("errorType", "not_found");
                model.addAttribute("errorTitle", "Paste Not Found");
                model.addAttribute("errorMessage", "The paste you're looking for doesn't exist or has been deleted.");
            } else if (errorMsg.contains("expired")) {
                model.addAttribute("errorType", "expired");
                model.addAttribute("errorTitle", "Paste Expired");
                model.addAttribute("errorMessage", "This paste has expired and is no longer available.");
                // Optional: Fetch paste again for details if needed, but since error, keep simple
                model.addAttribute("errorDetails", "The paste has reached its expiration time.");
            } else if (errorMsg.contains("limit reached")) {
                model.addAttribute("errorType", "limit_reached");
                model.addAttribute("errorTitle", "View Limit Reached");
                model.addAttribute("errorMessage", "This paste has reached its maximum view limit and is no longer available.");
                // For details, we'd need paste object; if critical, fetch separately, but to avoid extra DB call, use generic
                model.addAttribute("errorDetails", "The allowed number of views has been exceeded.");
            }
            return "error";
        }

        Paste paste = result.getPaste();
        Instant now = getNowInstant(testNowMs);

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