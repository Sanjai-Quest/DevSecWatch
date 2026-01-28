package com.devsecwatch.backend.repository;

import com.devsecwatch.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserIdIgnoreCaseOrderByCreatedAtDesc(String userId);

    long countByUserIdAndIsReadFalse(String userId);
}
