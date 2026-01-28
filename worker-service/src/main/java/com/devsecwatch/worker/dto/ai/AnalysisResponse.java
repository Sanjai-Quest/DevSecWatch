package com.devsecwatch.worker.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String description;
    private String fixSuggestion;
    private String confidence;
    private boolean isTemplate;
}
