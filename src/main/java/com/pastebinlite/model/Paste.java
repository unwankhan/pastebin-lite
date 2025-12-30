package com.pastebinlite.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Date;

@Document(collection = "pastes")
public class Paste {
    @Id
    private String id;

    private String content;

    @Indexed(unique = true)
    private String pasteId; // Short URL ID

    // JSON mapping for incoming/outgoing API fields
    @JsonProperty("ttl_seconds")
    private Integer ttlSeconds;

    @JsonProperty("max_views")
    private Integer maxViews;

    private Integer viewCount = 0;
    private Instant createdAt;

    // Indexed for optional TTL (Mongo can drop documents if configured)
    @Indexed(name = "expires_at_idx", expireAfterSeconds = 0)
    private Date expiresAt;

    private Instant lastAccessedAt;
    private boolean isActive = true;

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

    // Getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

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
        if (this.viewCount == null) this.viewCount = 0;
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
        return viewCount != null && viewCount >= maxViews;
    }

    public boolean isAvailable(Instant now) {
        return isActive && !isExpired(now) && !isViewLimitExceeded();
    }

    @JsonProperty("remaining_views")
    public Integer getRemainingViews() {
        if (maxViews == null) return null;
        int rem = Math.max(0, maxViews - (viewCount == null ? 0 : viewCount));
        return rem;
    }

    // Expose ISO string for expires_at in API responses
    @JsonProperty("expires_at")
    public String getExpiresAtISO() {
        if (expiresAt == null) return null;
        return expiresAt.toInstant().toString(); // ISO-8601 UTC
    }

    // Check if paste should be deactivated
    public boolean shouldDeactivate(Instant now) {
        return !isActive || isExpired(now) || isViewLimitExceeded();
    }
}
