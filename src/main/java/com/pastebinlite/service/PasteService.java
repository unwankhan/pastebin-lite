package com.pastebinlite.service;

import com.pastebinlite.model.Paste;
import com.pastebinlite.repository.PasteRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class PasteService {

    @Autowired
    private PasteRepository pasteRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @Value("${app.paste-id-length:8}")
    private int pasteIdLength;

    @Value("${app.base-url:}")
    private String baseUrl;

    // Read TEST_MODE env var (grader will set TEST_MODE=1)
    @Value("${TEST_MODE:0}")
    private String testModeEnv;

    private boolean isTestMode() {
        return "1".equals(testModeEnv) || "true".equalsIgnoreCase(testModeEnv);
    }

    public Paste createPaste(String content, Integer ttlSeconds, Integer maxViews) {
        Paste paste = new Paste(content, ttlSeconds, maxViews);
        paste.setPasteId(generatePasteId());
        // createdAt set in Paste constructor
        return pasteRepository.save(paste);
    }

    /**
     * API fetch: must count as a view. This method increments viewCount atomically (CAS loop),
     * and returns the updated Paste if available, or empty if not available.
     *
     * testNowMs: when TEST_MODE=1, the controller can pass x-test-now-ms to emulate time.
     */
    public Optional<Paste> getPaste(String pasteId, Long testNowMs) {
        Instant now = getNowInstant(testNowMs);

        // Retry loop: read-compare-and-update atomically using findAndModify with expected viewCount.
        final int MAX_RETRIES = 5;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Optional<Paste> opt = pasteRepository.findByPasteId(pasteId);
            if (opt.isEmpty()) return Optional.empty();
            Paste p = opt.get();

            // Check availability using "now"
            if (p.isExpired(now) || p.isViewLimitExceeded()) {
                return Optional.empty();
            }

            Integer prevViewCount = p.getViewCount();
            if (prevViewCount == null) prevViewCount = 0;

            // Build query: match pasteId, match same viewCount (CAS), and ensure not expired at query-time, and isActive true
            Query query = Query.query(
                    Criteria.where("pasteId").is(pasteId)
                            .and("isActive").is(true)
                            .andOperator(
                                    new Criteria().orOperator(
                                            Criteria.where("expiresAt").gt(java.util.Date.from(now)),
                                            Criteria.where("expiresAt").is(null)
                                    )
                            )
                            .and("viewCount").is(prevViewCount)
            );

            Update update = new Update()
                    .inc("viewCount", 1)
                    .set("lastAccessedAt", Instant.now());

            FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

            Paste updated = mongoOperations.findAndModify(query, update, options, Paste.class);
            if (updated != null) {
                // Successfully incremented and we have the updated document
                return Optional.of(updated);
            }
            // else another concurrent request likely updated viewCount — retry
        }

        // If we exhausted retries, return empty to signal unavailable / race condition
        return Optional.empty();
    }

    /**
     * For HTML view only: should NOT increment view count per assignment (API fetches count as views).
     * Returns the paste if available (no mutation).
     */
    public Optional<Paste> getPasteForViewOnly(String pasteId, Long testNowMs) {
        Optional<Paste> opt = pasteRepository.findByPasteId(pasteId);
        if (opt.isEmpty()) return Optional.empty();
        Paste p = opt.get();
        Instant now = getNowInstant(testNowMs);
        if (p.isExpired(now) || p.isViewLimitExceeded()) return Optional.empty();
        return Optional.of(p);
    }

    public String getPasteUrl(String pasteId) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            if (baseUrl.endsWith("/")) {
                return baseUrl + "p/" + pasteId;
            } else {
                return baseUrl + "/p/" + pasteId;
            }
        }
        // fallback
        return "/p/" + pasteId;
    }

    private String generatePasteId() {
        String pid;
        do {
            pid = RandomStringUtils.randomAlphanumeric(pasteIdLength).toLowerCase();
        } while (pasteRepository.findByPasteId(pid).isPresent());
        return pid;
    }

    private Instant getNowInstant(Long testNowMs) {
        if (isTestMode() && testNowMs != null) {
            return Instant.ofEpochMilli(testNowMs);
        }
        return Instant.now();
    }

    /**
     * Deactivate expired pastes (best-effort cleanup).
     */
    public void cleanupExpiredPastes() {
        Instant now = Instant.now();
        pasteRepository.findExpiredPastes(Date.from(now).toInstant()).forEach(paste -> {
            paste.setActive(false);
            pasteRepository.save(paste);
        });
    }
}
