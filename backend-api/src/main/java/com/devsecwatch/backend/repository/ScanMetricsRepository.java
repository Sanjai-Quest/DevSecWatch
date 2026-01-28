package com.devsecwatch.backend.repository;

import com.devsecwatch.backend.model.ScanMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScanMetricsRepository extends JpaRepository<ScanMetrics, Long> {
    Optional<ScanMetrics> findByScanId(Long scanId);
}
