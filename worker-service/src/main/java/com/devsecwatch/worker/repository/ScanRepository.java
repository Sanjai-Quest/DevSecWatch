package com.devsecwatch.worker.repository;

import com.devsecwatch.worker.model.Scan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScanRepository extends JpaRepository<Scan, Long> {
    @EntityGraph(attributePaths = { "user" })
    Optional<Scan> findById(Long id);
}
