package com.sjf.portal.dto;

public record WorldRecommendResponse(
        Long worldId,
        String city,
        String worldName,
        String description,
        String backgroundUrl,
        String reason
) {
}