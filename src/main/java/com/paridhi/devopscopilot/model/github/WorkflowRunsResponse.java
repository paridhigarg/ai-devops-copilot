package com.paridhi.devopscopilot.model.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowRunsResponse {

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("workflow_runs")
    private List<WorkflowRun> workflowRuns;
}
