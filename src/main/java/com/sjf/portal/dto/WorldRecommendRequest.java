package com.sjf.portal.dto;

public record WorldRecommendRequest(
        Long productId,
        String mood,
        String travelStyle
) {
}
