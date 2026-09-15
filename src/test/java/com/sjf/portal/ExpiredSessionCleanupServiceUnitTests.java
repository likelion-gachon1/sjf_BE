package com.sjf.portal;

import com.sjf.portal.domain.ColorwayKey;
import com.sjf.portal.domain.Journey;
import com.sjf.portal.domain.Mood;
import com.sjf.portal.domain.PortalSession;
import com.sjf.portal.domain.WorldId;
import com.sjf.portal.repository.PortalSessionRepository;
import com.sjf.portal.service.ExpiredSessionCleanupService;
import com.sjf.portal.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiredSessionCleanupServiceUnitTests {

    @Mock PortalSessionRepository repository;
    @Mock FileStorageService storage;
    @InjectMocks ExpiredSessionCleanupService cleanup;

    @Test
    void keepsRowWhenImageDeletionFailsSoNextRunCanRetry() {
        Instant now = Instant.now();
        PortalSession session = PortalSession.create(
                "expired-session-0002", true, "stark_backpack_visetos", ColorwayKey.PINK,
                Mood.CALM, Journey.EXPLORE, WorldId.PARIS_DAWN,
                "expired-session-0002.jpg", now, now, now.minusSeconds(1)
        );
        when(repository.findByExpiresAtBeforeOrderByExpiresAtAsc(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(session));
        when(storage.deleteIfExists(session.getImageFilename())).thenReturn(false);

        assertThat(cleanup.cleanupExpired(now)).isZero();
        verify(repository, never()).delete(any(PortalSession.class));
    }
}
