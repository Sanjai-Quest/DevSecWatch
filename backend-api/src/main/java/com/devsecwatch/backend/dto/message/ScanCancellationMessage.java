package com.devsecwatch.backend.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanCancellationMessage {
    private Long scanId;
    private String username;
    private String correlationId;
    private LocalDateTime timestamp;
}
