package com.sjf.portal.dto;

public record ProductResponse(
        Long productId,
        String name,
        String color,
        String imageUrl,
        String productUrl
) {
}