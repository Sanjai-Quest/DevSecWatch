package com.devsecwatch.backend.repository;

import com.devsecwatch.backend.model.Scan;
import com.devsecwatch.backend.model.User;
import com.devsecwatch.backend.model.enums.ScanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanRepository extends JpaRepository<Scan, Long> {
    Page<Scan> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Scan> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Scan> findByStatus(ScanStatus status);
}
