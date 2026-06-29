package com.devsecwatch.backend.controller;

import com.devsecwatch.backend.dto.scan.ScanRequest;
import com.devsecwatch.backend.dto.scan.ScanResponse;
import com.devsecwatch.backend.dto.scan.VulnerabilityResponse;
import com.devsecwatch.backend.service.ScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
@Slf4j
public class ScanController {

    private final ScanService scanService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ScanResponse> createScan(
            @Valid @RequestBody ScanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received scan request from user: {}", userDetails.getUsername());
        ScanResponse response = scanService.createScan(request, userDetails.getUsername());

        return ResponseEntity.accepted()
                .location(URI.create("/api/scans/" + response.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScanResponse> getScanById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(scanService.getScanById(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/vulnerabilities")
    public ResponseEntity<List<VulnerabilityResponse>> getVulnerabilitiesByScanId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(scanService.getVulnerabilitiesByScanId(id, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<ScanResponse>> getUserScans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(scanService.getUserScans(userDetails.getUsername(), pageable));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        scanService.deleteScan(id, userDetails.getUsername());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelScan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received cancellation request for scan ID: {} from user: {}", id, userDetails.getUsername());
        scanService.cancelScan(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
