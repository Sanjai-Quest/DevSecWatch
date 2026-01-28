package com.devsecwatch.worker.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIExplanation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String description;
    private String fixSuggestion;
    private String confidence;
    private boolean isTemplate;
}
