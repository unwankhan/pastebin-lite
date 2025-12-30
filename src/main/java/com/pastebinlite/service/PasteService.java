package com.pastebinlite.service;

import com.pastebinlite.model.Paste;
import com.pastebinlite.repository.PasteRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Optional;

@Service
public class PasteService {

    @Autowired
    private PasteRepository pasteRepository;

    @Value("${app.paste-id-length:8}")
    private int pasteIdLength;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${test.mode.enabled:false}")
    private boolean testModeEnabled;

    public Paste createPaste(String content, Integer ttlSeconds, Integer maxViews) {
        //System.out.println("Creating Paste" + content + " ttlSeconds"+ ttlSeconds + " maxViews" + maxViews);
        Paste paste = new Paste(content, ttlSeconds, maxViews);
        paste.setPasteId(generatePasteId());
        return pasteRepository.save(paste);
    }

    public Optional<Paste> getPaste(String pasteId, Long testNowMs) {
        Optional<Paste> pasteOpt = pasteRepository.findByPasteId(pasteId);

        if (pasteOpt.isEmpty()) {
            return Optional.empty();
        }

        Paste paste = pasteOpt.get();
        Instant now = getNowInstant(testNowMs);

        // Check if paste is expired or view limit reached
        if (paste.isExpired(now) || paste.isViewLimitExceeded()) {
            // Don't increment view count for expired/limit reached
            return Optional.empty();
        }

        // Increment view count
        paste.incrementViewCount();
        pasteRepository.save(paste);

        return Optional.of(paste);
    }

    public String getPasteUrl(String pasteId) {
        return baseUrl + "/p/" + pasteId;
    }

    private String generatePasteId() {
        String pasteId;
        do {
            pasteId = RandomStringUtils.randomAlphanumeric(pasteIdLength).toLowerCase();
        } while (pasteRepository.findByPasteId(pasteId).isPresent());
        return pasteId;
    }

    private Instant getNowInstant(Long testNowMs) {
        if (testModeEnabled && testNowMs != null) {
            return Instant.ofEpochMilli(testNowMs);
        }
        return Instant.now();
    }

    public void cleanupExpiredPastes() {
        Instant now = Instant.now();
        pasteRepository.findExpiredPastes(now).forEach(paste -> {
            paste.setActive(false);
            pasteRepository.save(paste);
        });
    }
}