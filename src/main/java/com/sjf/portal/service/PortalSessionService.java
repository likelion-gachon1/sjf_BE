package com.sjf.portal.service;

import com.sjf.portal.domain.ColorwayKey;
import com.sjf.portal.domain.Journey;
import com.sjf.portal.domain.Mood;
import com.sjf.portal.domain.PortalSession;
import com.sjf.portal.domain.WorldId;
import com.sjf.portal.dto.CreateSessionRequest;
import com.sjf.portal.dto.SessionResponse;
import com.sjf.portal.exception.PortalApiException;
import com.sjf.portal.repository.PortalSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PortalSessionService {

    private final PortalSessionRepository portalSessionRepository;
    private final FileStorageService fileStorageService;
    private final String frontendBaseUrl;
    private final String publicApiBaseUrl;
    private final long sessionTtlHours;

    public PortalSessionService(
            PortalSessionRepository portalSessionRepository,
            FileStorageService fileStorageService,
            @Value("${portal.frontend-base-url:http://localhost:3000}") String frontendBaseUrl,
            @Value("${portal.public-api-base-url:http://localhost:8080}") String publicApiBaseUrl,
            @Value("${portal.session-ttl-hours:24}") long sessionTtlHours
    ) {
        this.portalSessionRepository = portalSessionRepository;
        this.fileStorageService = fileStorageService;
        this.frontendBaseUrl = removeTrailingSlash(frontendBaseUrl);
        this.publicApiBaseUrl = removeTrailingSlash(publicApiBaseUrl);
        this.sessionTtlHours = sessionTtlHours;
    }

    @Transactional
    public SessionResponse create(CreateSessionRequest request, MultipartFile image) {
        if (!request.consent()) {
            throw new PortalApiException(HttpStatus.BAD_REQUEST, "촬영 이미지 활용 동의가 필요합니다.");
        }

        PortalSession existingSession = portalSessionRepository.findById(request.sessionId())
                .orElse(null);
        if (existingSession != null) {
            if (existingSession.getExpiresAt().isBefore(Instant.now())) {
                throw new PortalApiException(HttpStatus.GONE, "만료된 체험 결과입니다.");
            }
            return toResponse(existingSession);
        }

        ColorwayKey colorwayKey = parseEnum(ColorwayKey.class, request.colorwayKey(), "colorwayKey");
        Mood mood = parseEnum(Mood.class, request.mood(), "mood");
        Journey journey = parseEnum(Journey.class, request.journey(), "journey");
        WorldId worldId = parseEnum(WorldId.class, request.worldId(), "worldId");

        Instant capturedAt;
        try {
            capturedAt = Instant.ofEpochMilli(request.capturedAt());
        } catch (DateTimeException exception) {
            throw new PortalApiException(HttpStatus.BAD_REQUEST, "capturedAt 값이 올바르지 않습니다.");
        }

        String imageFilename = fileStorageService.saveJpeg(request.sessionId(), image);
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(sessionTtlHours, ChronoUnit.HOURS);

        PortalSession portalSession = PortalSession.create(
                request.sessionId(),
                request.consent(),
                request.productId(),
                colorwayKey,
                mood,
                journey,
                worldId,
                imageFilename,
                capturedAt,
                createdAt,
                expiresAt
        );

        try {
            PortalSession saved = portalSessionRepository.saveAndFlush(portalSession);
            return toResponse(saved);
        } catch (RuntimeException exception) {
            fileStorageService.deleteQuietly(imageFilename);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public SessionResponse get(String sessionId) {
        return toResponse(findValidSession(sessionId));
    }

    @Transactional(readOnly = true)
    public Resource loadImage(String sessionId) {
        PortalSession portalSession = findValidSession(sessionId);
        return fileStorageService.load(portalSession.getImageFilename());
    }

    private PortalSession findValidSession(String sessionId) {
        PortalSession portalSession = portalSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PortalApiException(
                        HttpStatus.NOT_FOUND,
                        "체험 결과를 찾을 수 없습니다."
                ));

        if (portalSession.getExpiresAt().isBefore(Instant.now())) {
            throw new PortalApiException(HttpStatus.GONE, "만료된 체험 결과입니다.");
        }

        return portalSession;
    }

    private SessionResponse toResponse(PortalSession session) {
        String sessionPath = "/api/v1/sessions/" + session.getSessionId();

        return new SessionResponse(
                session.getSessionId(),
                session.getProductId(),
                lower(session.getColorwayKey()),
                lower(session.getMood()),
                lower(session.getJourney()),
                lower(session.getWorldId()),
                session.getCapturedAt().toEpochMilli(),
                frontendBaseUrl + "/m/" + session.getSessionId(),
                publicApiBaseUrl + sessionPath + "/image",
                publicApiBaseUrl + sessionPath + "/download",
                session.getExpiresAt()
        );
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);

        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException exception) {
            String allowed = Arrays.stream(enumType.getEnumConstants())
                    .map(this::lower)
                    .collect(Collectors.joining(", "));
            throw new PortalApiException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 값이 올바르지 않습니다. 허용값: " + allowed
            );
        }
    }

    private String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
