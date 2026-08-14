package com.sjf.portal.dto;

import java.time.Instant;

public record SessionResponse(
        String sessionId,
        String productId,
        String colorwayKey,
        String mood,
        String journey,
        String worldId,
        long capturedAt,
        String shareUrl,
        String imageUrl,
        String downloadUrl,
        Instant expiresAt
) {
}
