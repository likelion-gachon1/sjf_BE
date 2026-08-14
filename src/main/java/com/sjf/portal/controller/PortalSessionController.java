package com.sjf.portal.controller;

import com.sjf.portal.dto.CreateSessionRequest;
import com.sjf.portal.dto.SessionResponse;
import com.sjf.portal.service.PortalSessionService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/sessions")
public class PortalSessionController {

    private final PortalSessionService portalSessionService;

    public PortalSessionController(PortalSessionService portalSessionService) {
        this.portalSessionService = portalSessionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> create(
            @Valid @RequestPart("metadata") CreateSessionRequest request,
            @RequestPart("image") MultipartFile image
    ) {
        SessionResponse response = portalSessionService.create(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{sessionId}")
    public SessionResponse get(@PathVariable String sessionId) {
        return portalSessionService.get(sessionId);
    }

    @GetMapping("/{sessionId}/image")
    public ResponseEntity<Resource> image(@PathVariable String sessionId) {
        Resource image = portalSessionService.loadImage(sessionId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFilename() + "\"")
                .body(image);
    }

    @GetMapping("/{sessionId}/download")
    public ResponseEntity<Resource> download(@PathVariable String sessionId) {
        Resource image = portalSessionService.loadImage(sessionId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFilename() + "\"")
                .body(image);
    }
}
