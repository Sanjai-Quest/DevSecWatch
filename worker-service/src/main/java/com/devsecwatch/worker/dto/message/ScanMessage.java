package com.devsecwatch.worker.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long scanId;
    private Long userId;
    private String repoUrl;
    private String branch;
    private String correlationId;
    private LocalDateTime timestamp;
}
