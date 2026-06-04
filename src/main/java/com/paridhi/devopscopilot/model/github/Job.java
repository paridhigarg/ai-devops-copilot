package com.paridhi.devopscopilot.model.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

    private Long id;
    private String name;
    private String status;
    private String conclusion;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("run_id")
    private Long runId;

    @JsonProperty("started_at")
    private String startedAt;

    @JsonProperty("completed_at")
    private String completedAt;

    private List<Step> steps;
}
