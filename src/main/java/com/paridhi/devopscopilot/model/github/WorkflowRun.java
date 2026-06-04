package com.paridhi.devopscopilot.model.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowRun {

    private Long id;
    private String name;

    @JsonProperty("head_branch")
    private String headBranch;

    @JsonProperty("head_sha")
    private String headSha;

    private String status;
    private String conclusion;

    @JsonProperty("workflow_id")
    private Long workflowId;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("run_number")
    private Integer runNumber;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}
