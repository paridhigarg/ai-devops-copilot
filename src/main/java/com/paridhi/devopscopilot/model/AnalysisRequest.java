package com.paridhi.devopscopilot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AnalysisRequest {

    @JsonProperty("repo_owner")
    private String repoOwner;

    @JsonProperty("repo_name")
    private String repoName;

    /**
     * Optional. If null, the latest failed run is fetched automatically.
     */
    @JsonProperty("run_id")
    private Long runId;

    /**
     * Branch to search latest failed run on. Defaults to "main".
     */
    @JsonProperty("branch")
    private String branch = "main";
}
