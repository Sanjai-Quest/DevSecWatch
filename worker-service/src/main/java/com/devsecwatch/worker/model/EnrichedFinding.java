package com.devsecwatch.worker.model;

import com.devsecwatch.worker.dto.ai.AIExplanation;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrichedFinding {
    private Finding finding;
    private AIExplanation explanation;
}
