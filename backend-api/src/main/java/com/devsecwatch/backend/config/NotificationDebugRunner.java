package com.devsecwatch.backend.config;

import com.devsecwatch.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDebugRunner implements CommandLineRunner {

    private final NotificationRepository notificationRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("================= NOTIFICATION DB DUMP ==================");
        notificationRepository.findAll().forEach(n -> {
            log.info("ID: {}, User: '{}', Title: '{}', Read: {}",
                    n.getId(), n.getUserId(), n.getTitle(), n.isRead());
        });
        log.info("=========================================================");
    }
}
