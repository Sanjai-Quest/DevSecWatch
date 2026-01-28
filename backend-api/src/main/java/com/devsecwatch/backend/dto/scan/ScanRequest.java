package com.devsecwatch.backend.dto.scan;

import com.devsecwatch.backend.validation.BranchName;
import com.devsecwatch.backend.validation.GitHubUrl;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanRequest {

    @NotBlank(message = "Repository URL is required")
    @GitHubUrl
    private String repoUrl;

    @BranchName
    private String branch = "main";
}
