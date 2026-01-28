package com.devsecwatch.worker.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    @JsonProperty("vulnerability_type")
    private String vulnerabilityType;

    @JsonProperty("code_snippet")
    private String codeSnippet;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("line_number")
    private int lineNumber;
}
