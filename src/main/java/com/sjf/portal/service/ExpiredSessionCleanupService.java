package com.sjf.portal.service;

import com.sjf.portal.domain.PortalSession;
import com.sjf.portal.repository.PortalSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ExpiredSessionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ExpiredSessionCleanupService.class);
    private static final int BATCH_SIZE = 100;

    private final PortalSessionRepository repository;
    private final FileStorageService fileStorageService;

    public ExpiredSessionCleanupService(
            PortalSessionRepository repository,
            FileStorageService fileStorageService
    ) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    @Scheduled(fixedDelayString = "${portal.cleanup-interval-ms:900000}")
    @Transactional
    public void scheduledCleanup() {
        int deleted = cleanupExpired(Instant.now());
        if (deleted > 0) log.info("만료된 MCM PORTAL 세션 {}건을 정리했습니다.", deleted);
    }

    /** 테스트와 수동 운영 점검에서 동일한 정리 규칙을 호출합니다. */
    @Transactional
    public int cleanupExpired(Instant cutoff) {
        int deleted = 0;
        while (true) {
            List<PortalSession> expired = repository.findByExpiresAtBeforeOrderByExpiresAtAsc(
                    cutoff, PageRequest.of(0, BATCH_SIZE)
            );
            if (expired.isEmpty()) return deleted;

            int deletedThisBatch = 0;
            for (PortalSession session : expired) {
                if (!fileStorageService.deleteIfExists(session.getImageFilename())) {
                    log.warn("만료 이미지 삭제에 실패해 다음 주기에 재시도합니다: {}", session.getSessionId());
                    continue;
                }
                repository.delete(session);
                deleted++;
                deletedThisBatch++;
            }
            repository.flush();
            // 한 배치 전체가 파일 오류면 같은 레코드를 무한 재조회하지 않습니다.
            if (deletedThisBatch == 0 || expired.size() < BATCH_SIZE) return deleted;
        }
    }
}
