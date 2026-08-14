package com.sjf.portal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "portal_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortalSession {

    @Id
    @Column(name = "session_id", length = 64, nullable = false, updatable = false)
    private String sessionId;

    @Column(nullable = false)
    private boolean consent;

    @Column(name = "product_id", length = 100, nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "colorway_key", length = 20, nullable = false)
    private ColorwayKey colorwayKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Mood mood;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Journey journey;

    @Enumerated(EnumType.STRING)
    @Column(name = "world_id", length = 40, nullable = false)
    private WorldId worldId;

    @Column(name = "image_filename", length = 100, nullable = false)
    private String imageFilename;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private PortalSession(
            String sessionId,
            boolean consent,
            String productId,
            ColorwayKey colorwayKey,
            Mood mood,
            Journey journey,
            WorldId worldId,
            String imageFilename,
            Instant capturedAt,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.sessionId = sessionId;
        this.consent = consent;
        this.productId = productId;
        this.colorwayKey = colorwayKey;
        this.mood = mood;
        this.journey = journey;
        this.worldId = worldId;
        this.imageFilename = imageFilename;
        this.capturedAt = capturedAt;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static PortalSession create(
            String sessionId,
            boolean consent,
            String productId,
            ColorwayKey colorwayKey,
            Mood mood,
            Journey journey,
            WorldId worldId,
            String imageFilename,
            Instant capturedAt,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new PortalSession(
                sessionId,
                consent,
                productId,
                colorwayKey,
                mood,
                journey,
                worldId,
                imageFilename,
                capturedAt,
                createdAt,
                expiresAt
        );
    }
}
