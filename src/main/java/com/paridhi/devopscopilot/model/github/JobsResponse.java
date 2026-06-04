package com.paridhi.devopscopilot.model.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobsResponse {

    @JsonProperty("total_count")
    private Integer totalCount;

    private List<Job> jobs;
}
