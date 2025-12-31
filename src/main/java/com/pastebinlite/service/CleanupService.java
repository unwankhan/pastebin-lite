package com.pastebinlite.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CleanupService {

    @Autowired
    private PasteService pasteService;

    // Run every hour
    @Scheduled(fixedRate = 1800000)
    public void cleanupExpiredPastes() {
        pasteService.cleanupExpiredPastes();
    }
}