package com.sjf.portal.repository;

import com.sjf.portal.domain.PortalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface PortalSessionRepository extends JpaRepository<PortalSession, String> {
    List<PortalSession> findByExpiresAtBeforeOrderByExpiresAtAsc(Instant cutoff, Pageable pageable);
}
