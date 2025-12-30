package com.pastebinlite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.time.Instant;
import java.util.Date;

@Document(collection = "pastes")
//@CompoundIndexes({
//        @CompoundIndex(name = "user_active_idx", def = "{'userId': 1, 'isActive': 1}"),
//        @CompoundIndex(name = "created_at_idx", def = "{'createdAt': -1}")
//})
public class Paste {
    @Id
    private String id;

    private String content;
    //private String userId;

    @Indexed(unique = true)
    private String pasteId; // Short URL ID

    private Integer ttlSeconds;
    private Integer maxViews;
    private Integer viewCount = 0;

    private Instant createdAt;

    @Indexed(name = "expires_at_idx", expireAfterSeconds = 0)
    private Date expiresAt;

    private Instant lastAccessedAt;
    private boolean isActive = false;

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

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

//    public String getUserId() { return userId; }
//    public void setUserId(String userId) { this.userId = userId; }

    public String getPasteId() { return pasteId; }
    public void setPasteId(String pasteId) { this.pasteId = pasteId; }

    public Integer getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        calculateExpiry();
    }

    public Integer getMaxViews() { return maxViews; }
    public void setMaxViews(Integer maxViews) { this.maxViews = maxViews; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public void incrementViewCount() {
        this.viewCount++;
        this.lastAccessedAt = Instant.now();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        calculateExpiry();
    }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(Instant lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public boolean isActive() { return isActive; }
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

    public boolean isAvailable(Instant now) {
        return isActive && !isExpired(now) && !isViewLimitExceeded();
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

    // Check if paste needs to be deactivated
    public boolean shouldDeactivate(Instant now) {
        return !isActive || isExpired(now) || isViewLimitExceeded();
    }
}