package com.paridhi.devopscopilot.controller;

import com.paridhi.devopscopilot.model.AnalysisRequest;
import com.paridhi.devopscopilot.model.AnalysisResponse;
import com.paridhi.devopscopilot.service.RCAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/analyze")
@RequiredArgsConstructor
public class AnalysisController {

    private final RCAService rcaService;

   
    @PostMapping
    public ResponseEntity<AnalysisResponse> analyze(@RequestBody AnalysisRequest request) {
        validateRequest(request);

        log.info("POST /analyze — repo: {}/{}, run_id: {}",
                request.getRepoOwner(), request.getRepoName(), request.getRunId());

        AnalysisResponse response = rcaService.analyze(
                request.getRepoOwner(),
                request.getRepoName(),
                request.getRunId(),
                request.getBranch() != null ? request.getBranch() : "main"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Convenience GET endpoint — analyses the latest failed run on the given branch.
     *
     * GET /api/v1/analyze/latest?owner=octocat&repo=my-repo&branch=main
     */
    @GetMapping("/latest")
    public ResponseEntity<AnalysisResponse> analyzeLatest(
            @RequestParam("owner") String owner,
            @RequestParam("repo") String repo,
            @RequestParam(value = "branch", defaultValue = "main") String branch) {

        validateOwnerRepo(owner, repo);

        log.info("GET /analyze/latest — repo: {}/{}, branch: {}", owner, repo, branch);

        AnalysisResponse response = rcaService.analyze(owner, repo, null, branch);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Input validation (OWASP: validate at system boundaries)
    // -------------------------------------------------------------------------

    private void validateRequest(AnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        validateOwnerRepo(request.getRepoOwner(), request.getRepoName());
    }

    /**
     * GitHub usernames and repo names allow only alphanumeric characters,
     * hyphens, underscores, and dots. Enforce this to prevent path injection
     * through the GitHub API URI templates.
     */
    private void validateOwnerRepo(String owner, String repo) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("repo_owner is required");
        }
        if (repo == null || repo.isBlank()) {
            throw new IllegalArgumentException("repo_name is required");
        }
        if (!owner.matches("[a-zA-Z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid repo_owner format");
        }
        if (!repo.matches("[a-zA-Z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid repo_name format");
        }
    }
}
