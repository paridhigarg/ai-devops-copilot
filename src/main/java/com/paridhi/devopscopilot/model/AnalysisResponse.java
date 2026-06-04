package com.paridhi.devopscopilot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalysisResponse {

    @JsonProperty("run_id")
    private Long runId;

    @JsonProperty("repo")
    private String repo;

    @JsonProperty("workflow_name")
    private String workflowName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("failed_jobs")
    private List<String> failedJobs;

    @JsonProperty("root_cause")
    private String rootCause;

    @JsonProperty("failed_steps")
    private List<String> failedSteps;

    @JsonProperty("recommendation")
    private String recommendation;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("analysis_timestamp")
    private String analysisTimestamp;
}
