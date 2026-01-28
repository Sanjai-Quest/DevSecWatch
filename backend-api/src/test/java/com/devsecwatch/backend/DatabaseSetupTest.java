package com.devsecwatch.backend;

import com.devsecwatch.backend.model.*;
import com.devsecwatch.backend.model.enums.*;
import com.devsecwatch.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // Make sure to use application-test.yml if you have one, or defaults
@Transactional // Rollback after each test
class DatabaseSetupTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @Autowired
    private ScanMetricsRepository scanMetricsRepository;

    @Test
    void testUserEntityPersistence() {
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getCreatedAt()); // Auditing check
        assertEquals("testuser", savedUser.getUsername());

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertTrue(found.isPresent());
    }

    @Test
    void testScanRelationshipAndCascading() {
        User user = User.builder()
                .username("scanuser")
                .email("scan@example.com")
                .passwordHash("pass")
                .build();
        userRepository.save(user);

        Scan scan = Scan.builder()
                .user(user)
                .repoUrl("http://github.com/example/repo")
                .status(ScanStatus.QUEUED)
                .build();

        Scan savedScan = scanRepository.save(scan);
        assertNotNull(savedScan.getId());

        // Test Vulnerability Cascade
        Vulnerability vuln = Vulnerability.builder()
                .scan(savedScan)
                .filePath("src/Main.java")
                .lineNumber(10)
                .vulnerabilityType("SQL Injection")
                .severity(Severity.HIGH)
                .confidence(ConfidenceLevel.HIGH)
                .description("Bad code")
                .codeSnippet("String query = ...")
                .fixSuggestion("Use prepared statements")
                .build();

        vulnerabilityRepository.save(vuln);

        assertEquals(1, vulnerabilityRepository.count());

        // Delete scan should delete vulnerability
        scanRepository.delete(savedScan);
        scanRepository.flush(); // Force delete

        assertEquals(0, vulnerabilityRepository.count());
    }

    @Test
    void testUniqueConstraints() {
        User user1 = User.builder().username("u1").email("e1@test.com").passwordHash("p").build();
        userRepository.save(user1);

        User user2 = User.builder().username("u1").email("e2@test.com").passwordHash("p").build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user2);
        });
    }

    @Test
    void testCustomRepositoryMethods() {
        User user = User.builder().username("metricsuser").email("m@test.com").passwordHash("p").build();
        userRepository.save(user);

        Scan scan1 = Scan.builder().user(user).repoUrl("r1").status(ScanStatus.COMPLETED).build();
        Scan scan2 = Scan.builder().user(user).repoUrl("r2").status(ScanStatus.FAILED).build();
        scanRepository.save(scan1);
        scanRepository.save(scan2);

        List<Scan> completed = scanRepository.findByStatus(ScanStatus.COMPLETED);
        assertEquals(1, completed.size());
        assertEquals("r1", completed.get(0).getRepoUrl());
    }
}
