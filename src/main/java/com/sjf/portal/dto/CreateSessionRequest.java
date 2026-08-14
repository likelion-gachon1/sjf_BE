package com.sjf.portal.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank(message = "sessionId는 필수입니다.")
        @Size(max = 64, message = "sessionId는 64자 이하여야 합니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9-]{16,64}$",
                message = "sessionId 형식이 올바르지 않습니다."
        )
        String sessionId,

        @AssertTrue(message = "촬영 이미지 활용 동의가 필요합니다.")
        boolean consent,

        @NotBlank(message = "productId는 필수입니다.")
        @Size(max = 100, message = "productId는 100자 이하여야 합니다.")
        String productId,

        @NotBlank(message = "colorwayKey는 필수입니다.")
        String colorwayKey,

        @NotBlank(message = "mood는 필수입니다.")
        String mood,

        @NotBlank(message = "journey는 필수입니다.")
        String journey,

        @NotBlank(message = "worldId는 필수입니다.")
        String worldId,

        @NotNull(message = "capturedAt은 필수입니다.")
        @Positive(message = "capturedAt은 0보다 커야 합니다.")
        Long capturedAt
) {
}
