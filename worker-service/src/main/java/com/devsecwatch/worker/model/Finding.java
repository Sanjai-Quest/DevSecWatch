package com.devsecwatch.worker.model;

import com.devsecwatch.worker.model.enums.Severity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Finding {
    private String filePath;
    private int lineNumber;
    private String vulnerabilityType;
    private Severity severity;
    private String description;
    private String codeSnippet;
    private String ruleId;
    private double semgrepConfidence;
    private String cveId;
}
