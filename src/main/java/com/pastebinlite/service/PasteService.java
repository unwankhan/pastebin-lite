package com.pastebinlite.service;

import com.pastebinlite.model.Paste;
import com.pastebinlite.repository.PasteRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    @Value("${app.paste-id-length:8}")
    private int pasteIdLength;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${test.mode:0}")
    private int testMode;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Paste createPaste(String content, Integer ttlSeconds, Integer maxViews) {
        //System.out.println("Creating Paste" + content + " ttlSeconds"+ ttlSeconds + " maxViews" + maxViews);
        Paste paste = new Paste(content, ttlSeconds, maxViews);
        paste.setPasteId(generatePasteId());
        return pasteRepository.save(paste);
    }

    public Optional<Paste> getPaste(String pasteId, Long testNowMs) {
        Instant now = getNowInstant(testNowMs);

        // First, check if paste exists and is available
        Optional<Paste> pasteOpt = pasteRepository.findByPasteId(pasteId);
        if (pasteOpt.isEmpty()) {
            return Optional.empty();
        }

        Paste paste = pasteOpt.get();

        // Check constraints before incrementing
        if (paste.isExpired(now)) {
            return Optional.empty();
        }

        if (paste.isViewLimitExceeded()) {
            paste.setActive(false);
            pasteRepository.save(paste);
            return Optional.empty();
        }

        // Atomic update using MongoDB's findAndModify
        Query query = new Query();
        query.addCriteria(Criteria.where("pasteId").is(pasteId)
                .and("isActive").is(true)
                .andOperator(
                        new Criteria().orOperator(
                                Criteria.where("expiresAt").is(null),
                                Criteria.where("expiresAt").gt(Date.from(now))
                        ),
                        new Criteria().orOperator(
                                Criteria.where("maxViews").is(null),
                                Criteria.where("viewCount").lt(paste.getMaxViews())
                        )
                ));

        Update update = new Update();
        update.inc("viewCount", 1);
        update.set("lastAccessedAt", now);

        Paste updatedPaste = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Paste.class
        );

        return Optional.ofNullable(updatedPaste);
    }

    public Optional<Paste> getPasteForViewOnly(String pasteId, Long testNowMs) {
    Optional<Paste> pasteOpt = pasteRepository.findByPasteId(pasteId);
    if (pasteOpt.isEmpty()) return Optional.empty();

    Paste paste = pasteOpt.get();
    Instant now = getNowInstant(testNowMs);

    if (paste.isExpired(now) || paste.isViewLimitExceeded()) {
        return Optional.empty();
    }

    return Optional.of(paste);
}


    public String getPasteUrl(String pasteId) {
    if (baseUrl != null && !baseUrl.isBlank()) {
        // ensure no double slash
        if (baseUrl.endsWith("/")) {
            return baseUrl + "p/" + pasteId;
        } else {
            return baseUrl + "/p/" + pasteId;
        }
    }

    return "/p/" + pasteId;
}


    private String generatePasteId() {
        String pasteId;
        do {
            pasteId = RandomStringUtils.randomAlphanumeric(pasteIdLength).toLowerCase();
        } while (pasteRepository.findByPasteId(pasteId).isPresent());
        return pasteId;
    }

    public Instant getNowInstant(Long testNowMs) {
        // Validate header value
        if (testMode == 1 && testNowMs != null && testNowMs > 0) {
            try {
                return Instant.ofEpochMilli(testNowMs);
            } catch (Exception e) {
                // If invalid timestamp, fall back to current time
                return Instant.now();
            }
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

    public Optional<Paste> findPasteByPasteId(String id) {
        return pasteRepository.findByPasteId(id);
    }
}
