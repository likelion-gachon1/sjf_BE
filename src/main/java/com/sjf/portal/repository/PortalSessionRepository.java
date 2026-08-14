package com.sjf.portal.repository;

import com.sjf.portal.domain.PortalSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalSessionRepository extends JpaRepository<PortalSession, String> {
}
