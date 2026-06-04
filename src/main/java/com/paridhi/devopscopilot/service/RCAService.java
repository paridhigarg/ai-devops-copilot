package com.paridhi.devopscopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paridhi.devopscopilot.model.AnalysisResponse;
import com.paridhi.devopscopilot.model.github.Job;
import com.paridhi.devopscopilot.model.github.WorkflowRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RCAService {

    private static final int MAX_PROMPT_LOG_CHARS = 15_000;

    private final ChatClient chatClient;
    private final GitHubService gitHubService;
    private final ObjectMapper objectMapper;

    
    public RCAService(ChatClient.Builder chatClientBuilder,
                      GitHubService gitHubService,
                      ObjectMapper objectMapper) {

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a senior DevOps engineer and AI assistant who specialises in
                        CI/CD pipeline failure analysis. When given workflow run details and logs,
                        you identify root causes precisely and return only valid JSON — no markdown
                        fences, no explanatory text outside the JSON object.
                        """)
                .build();

        this.gitHubService = gitHubService;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public AnalysisResponse analyze(String owner, String repo, Long runId, String branch) {

        // 1. Resolve the workflow run
        WorkflowRun run = (runId != null)
                ? gitHubService.getWorkflowRun(owner, repo, runId)
                : gitHubService.getLatestFailedRun(owner, repo, branch);

        log.info("Analysing run #{} ({}) — conclusion: {}",
                run.getRunNumber(), run.getName(), run.getConclusion());

        // 2. Identify failed jobs
        List<Job> allJobs = gitHubService.getJobsForRun(owner, repo, run.getId());
        List<String> failedJobNames = allJobs.stream()
                .filter(j -> "failure".equals(j.getConclusion()))
                .map(Job::getName)
                .collect(Collectors.toList());

        // 3. Fetch raw logs (truncated per-job + capped total)
        String rawLogs = gitHubService.getFailedJobLogs(owner, repo, run.getId());
        String cappedLogs = rawLogs.length() > MAX_PROMPT_LOG_CHARS
                ? rawLogs.substring(rawLogs.length() - MAX_PROMPT_LOG_CHARS)
                : rawLogs;

        // 4. Call the LLM
        String aiJson = callAI(run, failedJobNames, cappedLogs);

        // 5. Parse and return
        return buildResponse(aiJson, run, owner, repo, failedJobNames);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String callAI(WorkflowRun run, List<String> failedJobs, String logs) {
        String prompt = buildPrompt(run, failedJobs, logs);
        log.info("Sending prompt to LLM (log snippet length: {} chars)...", logs.length());

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String buildPrompt(WorkflowRun run, List<String> failedJobs, String logs) {
        return String.format("""
                Analyse this GitHub Actions CI/CD pipeline failure and return a JSON object.

                WORKFLOW DETAILS:
                  Workflow : %s
                  Run #    : %s
                  Branch   : %s
                  Conclusion: %s
                  Failed jobs: %s
                  URL      : %s

                FAILURE LOGS (last portion):
                %s

                Return ONLY valid JSON with exactly these keys — no extra text:
                {
                  "root_cause":   "<concise 1-2 sentence root cause>",
                  "failed_steps": ["<step name>", ...],
                  "recommendation": "<specific actionable fix>",
                  "severity":     "HIGH | MEDIUM | LOW",
                  "summary":      "<2-3 sentence plain-English summary>"
                }
                """,
                run.getName(),
                run.getRunNumber(),
                run.getHeadBranch(),
                run.getConclusion(),
                String.join(", ", failedJobs),
                run.getHtmlUrl(),
                logs
        );
    }

    @SuppressWarnings("unchecked")
    private AnalysisResponse buildResponse(String aiJson,
                                           WorkflowRun run,
                                           String owner,
                                           String repo,
                                           List<String> failedJobNames) {
        // Defaults in case parsing fails
        String rootCause    = "AI analysis could not determine root cause";
        String recommendation = "Please review the workflow logs manually";
        String severity     = "HIGH";
        String summary      = "The workflow failed. Manual investigation is required.";
        List<String> failedSteps = List.of();

        try {
            // Strip markdown fences that some models add despite instructions
            String cleaned = aiJson.trim()
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)```\\s*$", "")
                    .trim();

            Map<String, Object> parsed = objectMapper.readValue(cleaned, Map.class);

            rootCause     = (String) parsed.getOrDefault("root_cause", rootCause);
            recommendation = (String) parsed.getOrDefault("recommendation", recommendation);
            severity      = (String) parsed.getOrDefault("severity", severity);
            summary       = (String) parsed.getOrDefault("summary", summary);

            Object steps = parsed.get("failed_steps");
            if (steps instanceof List<?> list) {
                failedSteps = list.stream().map(Object::toString).collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.warn("Could not parse LLM JSON response, storing raw text as root_cause. Error: {}",
                    e.getMessage());
            rootCause = aiJson;
        }

        return AnalysisResponse.builder()
                .runId(run.getId())
                .repo(owner + "/" + repo)
                .workflowName(run.getName())
                .status(run.getConclusion())
                .failedJobs(failedJobNames)
                .rootCause(rootCause)
                .failedSteps(failedSteps)
                .recommendation(recommendation)
                .severity(severity)
                .summary(summary)
                .analysisTimestamp(Instant.now().toString())
                .build();
    }
}
