package com.pastebinlite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.time.Instant;
import java.util.Date;

@Document(collection = "pastes")
public class Paste {
    @Id
    private String id;

    private String content;

    @Indexed(unique = true)
    private String pasteId; // Short URL ID

    private Integer ttlSeconds;
    private Integer maxViews;
    private Integer viewCount = 0;

    private Instant createdAt;

    @Indexed(name = "expires_at_idx", expireAfterSeconds = 0)
    private Date expiresAt;

    private boolean isActive;

    // Constructors
    public Paste() {
        this.createdAt = Instant.now();
        this.viewCount = 0;
        this.isActive = true;
    }

    public Paste(String content, Integer ttlSeconds, Integer maxViews) {
        this();
        this.content = content;
        this.ttlSeconds = ttlSeconds;
        this.maxViews = maxViews;
        calculateExpiry();
    }

    public String getContent() { return content; }


    public String getPasteId() { return pasteId; }
    public void setPasteId(String pasteId) { this.pasteId = pasteId; }



    public Integer getMaxViews() { return maxViews; }

    public Integer getViewCount() { return viewCount; }


    public Instant getCreatedAt() { return createdAt; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
    public void setActive(boolean active) { isActive = active; }

    // Helper methods
    public void calculateExpiry() {
        if (ttlSeconds != null && ttlSeconds > 0 && createdAt != null) {
            Instant expiryInstant = createdAt.plusSeconds(ttlSeconds);
            this.expiresAt = Date.from(expiryInstant);
        } else {
            this.expiresAt = null;
        }
    }

    public boolean isExpired(Instant now) {
        if (expiresAt == null) return false;
        return now.isAfter(expiresAt.toInstant());
    }

    public boolean isViewLimitExceeded() {
        if (maxViews == null) return false;
        return viewCount >= maxViews;
    }


    public Integer getRemainingViews() {
        if (maxViews == null) return null;
        return Math.max(0, maxViews - viewCount);
    }

    // Additional helper for JSON serialization
    public String getExpiresAtISO() {
        if (expiresAt == null) return null;
        return expiresAt.toInstant().toString();
    }

}