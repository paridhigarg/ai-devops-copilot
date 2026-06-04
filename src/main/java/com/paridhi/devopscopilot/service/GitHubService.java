package com.paridhi.devopscopilot.service;

import com.paridhi.devopscopilot.model.github.Job;
import com.paridhi.devopscopilot.model.github.JobsResponse;
import com.paridhi.devopscopilot.model.github.WorkflowRun;
import com.paridhi.devopscopilot.model.github.WorkflowRunsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitHubService {

    private static final int LOG_TRUNCATE_CHARS = 8_000;

    private final WebClient githubWebClient;

    /**
     * Plain WebClient (no auth headers) used only for following GitHub's
     * 302 redirect to the S3 pre-signed log URL. Sending the Authorization
     * header to S3 would cause a SignatureDoesNotMatch error.
     */
    private final WebClient plainWebClient;

    public GitHubService(WebClient githubWebClient) {
        this.githubWebClient = githubWebClient;
        this.plainWebClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public WorkflowRun getLatestFailedRun(String owner, String repo, String branch) {
        log.info("Fetching latest failed workflow run for {}/{} on branch '{}'", owner, repo, branch);

        WorkflowRunsResponse response = githubWebClient.get()
                .uri("/repos/{owner}/{repo}/actions/runs?branch={branch}&status=failure&per_page=1",
                        owner, repo, branch)
                .retrieve()
                .bodyToMono(WorkflowRunsResponse.class)
                .block();

        if (response == null
                || response.getWorkflowRuns() == null
                || response.getWorkflowRuns().isEmpty()) {
            throw new RuntimeException(
                    "No failed workflow runs found for " + owner + "/" + repo + " on branch '" + branch + "'");
        }

        return response.getWorkflowRuns().get(0);
    }

    public WorkflowRun getWorkflowRun(String owner, String repo, Long runId) {
        log.info("Fetching workflow run {} for {}/{}", runId, owner, repo);

        WorkflowRun run = githubWebClient.get()
                .uri("/repos/{owner}/{repo}/actions/runs/{runId}", owner, repo, runId)
                .retrieve()
                .bodyToMono(WorkflowRun.class)
                .block();

        if (run == null) {
            throw new RuntimeException("Workflow run " + runId + " not found in " + owner + "/" + repo);
        }
        return run;
    }

    public List<Job> getJobsForRun(String owner, String repo, Long runId) {
        log.info("Fetching jobs for run {} in {}/{}", runId, owner, repo);

        JobsResponse response = githubWebClient.get()
                .uri("/repos/{owner}/{repo}/actions/runs/{runId}/jobs", owner, repo, runId)
                .retrieve()
                .bodyToMono(JobsResponse.class)
                .block();

        return (response != null && response.getJobs() != null) ? response.getJobs() : List.of();
    }

    /**
     * Fetches raw log text for a single job.
     * GitHub returns a 302 redirect to an S3 pre-signed URL.
     * We extract the Location header and fetch from S3 without auth headers.
     */
    public String getJobLogs(String owner, String repo, Long jobId) {
        log.info("Fetching logs for job {} in {}/{}", jobId, owner, repo);

        try {
            return githubWebClient.get()
                    .uri("/repos/{owner}/{repo}/actions/jobs/{jobId}/logs", owner, repo, jobId)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is3xxRedirection()) {
                            String location = response.headers()
                                    .header("Location")
                                    .stream()
                                    .findFirst()
                                    .orElse(null);

                            if (location != null) {
                                // Consume the redirect response body first, then fetch logs
                                return response.releaseBody().then(
                                        plainWebClient.get()
                                                .uri(location)
                                                .retrieve()
                                                .bodyToMono(String.class)
                                                .onErrorReturn("Could not download log from redirect URL")
                                );
                            }
                        }
                        return response.bodyToMono(String.class);
                    })
                    .onErrorReturn("Log retrieval failed for job: " + jobId)
                    .block();

        } catch (Exception e) {
            log.warn("Could not fetch logs for job {}: {}", jobId, e.getMessage());
            return "Logs unavailable for job " + jobId;
        }
    }

    /**
     * Collects logs from all failed jobs in a run.
     * Truncates each job's log to the last LOG_TRUNCATE_CHARS characters
     * (errors almost always appear at the end of CI logs).
     */
    public String getFailedJobLogs(String owner, String repo, Long runId) {
        List<Job> allJobs = getJobsForRun(owner, repo, runId);

        List<Job> failedJobs = allJobs.stream()
                .filter(j -> "failure".equals(j.getConclusion())
                        || "cancelled".equals(j.getConclusion()))
                .collect(Collectors.toList());

        // If no explicitly failed jobs, analyse all jobs (run may still be in_progress)
        if (failedJobs.isEmpty()) {
            failedJobs = allJobs;
        }

        StringBuilder logsBuilder = new StringBuilder();

        for (Job job : failedJobs) {
            logsBuilder.append("=== JOB: ").append(job.getName())
                    .append(" [").append(job.getConclusion()).append("] ===\n");

            // Append failed step names for quick context
            if (job.getSteps() != null) {
                job.getSteps().stream()
                        .filter(s -> "failure".equals(s.getConclusion())
                                || "cancelled".equals(s.getConclusion()))
                        .forEach(s -> logsBuilder.append("  FAILED STEP: ")
                                .append(s.getName()).append("\n"));
            }

            String logs = getJobLogs(owner, repo, job.getId());
            if (logs != null && logs.length() > LOG_TRUNCATE_CHARS) {
                logs = "...[log truncated — showing last " + LOG_TRUNCATE_CHARS + " chars]...\n"
                        + logs.substring(logs.length() - LOG_TRUNCATE_CHARS);
            }
            logsBuilder.append(logs).append("\n\n");
        }

        return logsBuilder.toString();
    }
}
