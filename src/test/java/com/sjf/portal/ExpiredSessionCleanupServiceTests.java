package com.sjf.portal;

import com.sjf.portal.domain.ColorwayKey;
import com.sjf.portal.domain.Journey;
import com.sjf.portal.domain.Mood;
import com.sjf.portal.domain.PortalSession;
import com.sjf.portal.domain.WorldId;
import com.sjf.portal.repository.PortalSessionRepository;
import com.sjf.portal.service.ExpiredSessionCleanupService;
import com.sjf.portal.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
class ExpiredSessionCleanupServiceTests {

    @TempDir
    static Path storageRoot;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("portal.storage-path", () -> storageRoot.toString());
    }

    @Autowired PortalSessionRepository repository;
    @Autowired FileStorageService storage;
    @Autowired ExpiredSessionCleanupService cleanup;

    @BeforeEach
    void reset() {
        repository.deleteAll();
    }

    @Test
    void removesOnlyExpiredRowsAndImages() {
        Instant now = Instant.now();
        saveSession("expired-session-0001", now.minusSeconds(60));
        saveSession("active-session-00001", now.plusSeconds(3600));

        assertThat(cleanup.cleanupExpired(now)).isEqualTo(1);
        assertThat(repository.existsById("expired-session-0001")).isFalse();
        assertThat(repository.existsById("active-session-00001")).isTrue();
        assertThatThrownBy(() -> storage.load("expired-session-0001.jpg"))
                .isInstanceOf(RuntimeException.class);
        assertThat(storage.load("active-session-00001.jpg").exists()).isTrue();
    }

    private void saveSession(String id, Instant expiresAt) {
        String filename = storage.saveJpeg(id, new MockMultipartFile(
                "image", id + ".jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, 0, 0}
        ));
        Instant now = Instant.now();
        repository.saveAndFlush(PortalSession.create(
                id, true, "stark_backpack_visetos", ColorwayKey.PINK,
                Mood.CALM, Journey.EXPLORE, WorldId.PARIS_DAWN,
                filename, now, now, expiresAt
        ));
    }
}
