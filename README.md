# AI DevOps Co-Pilot 🤖

> **v0** — LLM-powered GitHub Actions failure analyser  
> Built with Java 17 · Spring Boot 3 · Spring AI · OpenAI GPT-4o-mini

## What it does

Point it at any GitHub repository and it will:

1. Fetch the latest (or a specific) failed GitHub Actions workflow run
2. Pull the raw logs from every failed job
3. Send them to GPT-4o via **Spring AI**
4. Return a structured Root Cause Analysis — root cause, failed steps, severity, and a plain-English summary anyone can understand

```
POST /api/v1/analyze
→ "The Docker build failed because the base image 'node:18-alpine' was not
   found in the registry. Update the Dockerfile to use 'node:20-alpine'."
```

---

## Architecture

```
Client
  │
  ▼
AnalysisController  (Spring MVC REST)
  │
  ├── GitHubService  ──────►  GitHub REST API v3
  │     • Lists workflow runs          (auth: GitHub PAT)
  │     • Fetches job metadata
  │     • Downloads job logs (302 → S3 pre-signed URL)
  │
  └── RCAService  ────────►  OpenAI GPT-4o
        • Builds structured prompt      (via Spring AI ChatClient)
        • Parses JSON response
        • Returns AnalysisResponse
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.9+ |
| Docker & Docker Compose | any recent version |
| OpenAI API key | [platform.openai.com](https://platform.openai.com) |
| GitHub Personal Access Token | `repo` + `actions:read` scopes |

---

## Quick start (Docker — recommended)

```bash
# 1. Clone
git clone https://github.com/<your-username>/ai-devops-copilot.git
cd ai-devops-copilot

# 2. Create your .env from the example
cp .env.example .env
# Edit .env and add your OPENAI_API_KEY and GITHUB_TOKEN

# 3. Run
docker-compose up --build

# 4. Verify it is running
curl http://localhost:8080/actuator/health
```

---

## Quick start (local Maven)

```bash
export OPENAI_API_KEY=sk-...
export GITHUB_TOKEN=ghp_...

mvn spring-boot:run
```

---

## API Reference

### `POST /api/v1/analyze` — Analyse a specific or latest run

**Request body:**
```json
{
  "repo_owner": "octocat",
  "repo_name":  "my-repo",
  "run_id":     12345678,
  "branch":     "main"
}
```

> `run_id` and `branch` are optional.  
> When `run_id` is omitted, the latest failed run on `branch` (default `main`) is used.

**Response:**
```json
{
  "run_id":            12345678,
  "repo":              "octocat/my-repo",
  "workflow_name":     "CI",
  "status":            "failure",
  "failed_jobs":       ["build-and-test"],
  "root_cause":        "Unit test 'UserServiceTest#createUser' failed with a NullPointerException because the mocked UserRepository was not initialised.",
  "failed_steps":      ["Run tests"],
  "recommendation":    "Add @MockBean for UserRepository in UserServiceTest and initialise the mock in @BeforeEach.",
  "severity":          "HIGH",
  "summary":           "The CI pipeline failed during the test phase. A missing mock caused a NullPointerException in the user-service unit tests. Fix the test setup and re-run the workflow.",
  "analysis_timestamp": "2026-05-26T10:30:00Z"
}
```

### `GET /api/v1/analyze/latest?owner=octocat&repo=my-repo&branch=main`

Same response as above — convenience endpoint for quick CLI use.

```bash
curl "http://localhost:8080/api/v1/analyze/latest?owner=octocat&repo=my-repo"
```

---

## Project structure

```
ai-devops-copilot/
├── src/main/java/com/paridhi/devopscopilot/
│   ├── DevOpsCopilotApplication.java
│   ├── config/
│   │   └── AppConfig.java              # WebClient bean (GitHub API)
│   ├── controller/
│   │   └── AnalysisController.java     # REST endpoints + input validation
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   ├── model/
│   │   ├── AnalysisRequest.java
│   │   ├── AnalysisResponse.java
│   │   ├── ErrorResponse.java
│   │   └── github/                     # GitHub API response DTOs
│   │       ├── Job.java
│   │       ├── JobsResponse.java
│   │       ├── Step.java
│   │       ├── WorkflowRun.java
│   │       └── WorkflowRunsResponse.java
│   └── service/
│       ├── GitHubService.java          # GitHub API client
│       └── RCAService.java             # Spring AI + OpenAI integration
├── Dockerfile                          # Multi-stage build
├── docker-compose.yml
├── .env.example
└── pom.xml
```

---

## Roadmap

| Version | What's coming |
|---|---|
| **v0** ✅ | LLM-powered CI/CD failure RCA via REST API |
| **v1** | LangGraph-style agent loop — Prometheus alert → reason → suggest fix |
| **v2** | Auto-remediation: restart pods, rollback deployments, trigger Ansible |
| **v3** | Multi-agent: Analyst + Executor + Notifier (Slack/email) |
| **v4** | Memory (Redis), PR security review, cost analysis |

---

## LinkedIn post — v0 launch

> **I built an AI agent that reads my CI/CD failures so I don't have to.**
>
> As a Java/DevOps engineer I've spent hours staring at 3000-line pipeline logs trying to find the one line that broke everything.
>
> So I built **AI DevOps Co-Pilot** — a Spring Boot service that:
> ✅ Connects to your GitHub Actions via REST API
> ✅ Pulls the raw logs from every failed job
> ✅ Sends them to GPT-4o through Spring AI
> ✅ Returns a clean JSON with: root cause, failed steps, severity, and a plain-English summary
>
> Stack: Java 17 · Spring Boot 3 · Spring AI · OpenAI GPT-4o-mini · Docker
>
> This is v0. The plan is to evolve it into a full multi-agent system that can not just *explain* failures but *fix* them autonomously.
>
> GitHub link in the comments 👇
>
> #Java #SpringBoot #SpringAI #DevOps #CI_CD #AIAgents #OpenAI #SoftwareEngineering #GitHub

---

## Security notes

- Secrets are loaded exclusively from environment variables — never hardcoded
- GitHub PAT scope is limited to `repo` + `actions:read`
- Input validation on all API endpoints prevents path injection
- Docker container runs as a non-root user




## Step-by-Step Implementation Guide

---

### Step 1: Install Prerequisites

**Java 17**
Download from [adoptium.net](https://adoptium.net) → Eclipse Temurin 17 (LTS). After installing:
```powershell
java -version
# Should print: openjdk version "17.x.x"
```

**Maven 3.9+**
Download from [maven.apache.org](https://maven.apache.org/download.cgi). Extract it, add `bin/` to your system `PATH`. Then:
```powershell
mvn -version
# Should print: Apache Maven 3.9.x
```

**Docker Desktop**
Download from [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop). Start Docker Desktop before running any Docker commands.
```powershell
docker --version
docker-compose --version
```

---

### Step 2: Get Your API Keys

**OpenAI API Key:**
1. Go to [platform.openai.com](https://platform.openai.com)
2. Sign up / Log in
3. Click your profile → **API keys** → **Create new secret key**
4. Copy it immediately — it's shown only once. Looks like: `sk-proj-abc123...`
5. Add a small **usage limit** (e.g. $5/month) under Billing → Usage limits — good safety net for personal projects

**GitHub Personal Access Token (PAT):**
1. Go to GitHub → **Settings** (top-right avatar)
2. Scroll down → **Developer settings** → **Personal access tokens** → **Fine-grained tokens**
3. Click **Generate new token**
4. Set name: `ai-devops-copilot`
5. Set expiration: 90 days
6. Under **Repository access** → select the repo(s) you want to analyse, OR choose **All repositories**
7. Under **Permissions**, set:
   - `Actions` → **Read-only**
   - `Contents` → **Read-only** (needed to read repo info)
8. Click **Generate token** → copy it. Looks like: `github_pat_abc123...`

---

### Step 3: Open the Project

Open VS Code, then:
```powershell
# Open the project folder
code C:\Users\paridhi.garg\ai-devops-copilot
```

Or in VS Code: **File → Open Folder** → navigate to `ai-devops-copilot`.

Install the **Extension Pack for Java** in VS Code if you haven't — it gives you Maven support, syntax highlighting, and run buttons.

---

### Step 4: Create Your `.env` File

In the project root (`ai-devops-copilot/`), copy the example:
```powershell
cd C:\Users\paridhi.garg\ai-devops-copilot
copy .env.example .env
```

Open `.env` and fill in your real values:
```
OPENAI_API_KEY=sk-proj-your-real-key-here
GITHUB_TOKEN=github_pat_your-real-token-here
```

**Important:** `.env` is already in `.gitignore` — it will never be pushed to GitHub. `.env.example` (with fake placeholder values) IS committed, so other people know what variables are needed.

---

### Step 5: Build the Project

```powershell
cd C:\Users\paridhi.garg\ai-devops-copilot
mvn clean package -DskipTests
```

What this does:
- `clean` — deletes the `target/` folder (previous build artifacts)
- `package` — compiles all Java files, runs processors (Lombok), packages everything into a single fat JAR: `target/ai-devops-copilot-0.1.0.jar`
- `-DskipTests` — skips tests for now (the context load test needs a real OpenAI key)

First run takes 2–4 minutes — Maven downloads all dependencies (~150MB). Subsequent builds are seconds.

You should see:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45.3 s
```

---

### Step 6: Run the Project

**Option A — Docker Compose (recommended, closest to production):**
```powershell
# This reads your .env file automatically
docker-compose up --build
```

What happens:
1. Docker builds the image using the multi-stage `Dockerfile` (JDK stage compiles, JRE stage runs)
2. Docker Compose injects `OPENAI_API_KEY` and `GITHUB_TOKEN` from your `.env`
3. Spring Boot starts on port `8080`

You should see:
```
ai-devops-copilot  | Started DevOpsCopilotApplication in 4.3 seconds
```

**Option B — Run directly with Maven (faster for development):**
```powershell
# Set env vars in PowerShell first
$env:OPENAI_API_KEY = "sk-proj-your-key"
$env:GITHUB_TOKEN   = "github_pat_your-token"

mvn spring-boot:run
```

---

### Step 7: Verify It's Running

```powershell
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP","components":{"ping":{"status":"UP"}}}
```

---

### Step 8: Make Your First Real API Call

**Find a repo with a failed workflow run.** You can use your own repo or any public repo that has CI failures. For example, let's say your GitHub username is `paridhi-garg` and you have a repo `my-spring-app` with a recent failure.

**Option A — GET (easiest, no body needed):**
```powershell
curl "http://localhost:8080/api/v1/analyze/latest?owner=paridhi-garg&repo=my-spring-app&branch=main"
```

**Option B — POST with a specific run ID:**

First find a run ID — go to `github.com/paridhi-garg/my-spring-app/actions`, click a failed run, look at the URL: `github.com/.../actions/runs/`**`14523891234`** — that number is the run ID.

```powershell
curl -X POST http://localhost:8080/api/v1/analyze `
  -H "Content-Type: application/json" `
  -d '{
    "repo_owner": "paridhi-garg",
    "repo_name":  "my-spring-app",
    "run_id":     14523891234
  }'
```

**Expected response:**
```json
{
  "run_id": 14523891234,
  "repo": "paridhi-garg/my-spring-app",
  "workflow_name": "CI",
  "status": "failure",
  "failed_jobs": ["build-and-test"],
  "root_cause": "The Maven build failed because dependency 'com.example:utils:1.0.0' could not be resolved from any configured repository.",
  "failed_steps": ["Build with Maven"],
  "recommendation": "Check that the dependency exists in Maven Central or your private registry. Verify the version number in pom.xml matches what is published.",
  "severity": "HIGH",
  "summary": "The CI pipeline failed during the build phase. Maven could not download a required dependency, likely due to an incorrect version or missing repository configuration. Update the dependency version and re-run.",
  "analysis_timestamp": "2026-05-26T10:30:00Z"
}
```

---

### Step 9: Test With Postman (optional, easier than curl)

1. Open Postman → **New Request**
2. Method: `POST`, URL: `http://localhost:8080/api/v1/analyze`
3. Body → **raw** → **JSON**:
```json
{
  "repo_owner": "paridhi-garg",
  "repo_name": "my-spring-app"
}
```
4. Hit **Send**

---

### Step 10: Push to GitHub

```powershell
cd ai-devops-copilot

# Initialize git
git init
git add .
git status   # verify .env is NOT listed (it should be ignored)
git commit -m "feat: v0 - AI-powered CI/CD failure analyser (Spring AI + GPT-4o-mini)"
```

Create a **new repository** on github.com (call it `ai-devops-copilot`, make it **public** for your portfolio):

```powershell
git remote add origin https://github.com/paridhi-garg/ai-devops-copilot.git
git branch -M main
git push -u origin main
```

**Before pushing, double-check:**
```powershell
git status          # .env should not appear
git log --oneline   # should show your commit
```

---

### Step 11: Post on LinkedIn

Copy the draft from the bottom of README.md — it's in the **LinkedIn post** section. Attach a screenshot of the JSON response (blank out the run ID if it's a private repo). Put the GitHub link in the first comment.

---

### Full flow summary

```
Install Java 17 + Maven + Docker
        ↓
Get OpenAI key + GitHub PAT
        ↓
Create .env with real keys
        ↓
mvn clean package -DskipTests
        ↓
docker-compose up --build
        ↓
curl /actuator/health  →  {"status":"UP"}
        ↓
curl /api/v1/analyze/latest?owner=...&repo=...
        ↓
Get structured RCA JSON  ✅
        ↓
git push → LinkedIn post
```

---

### Common issues and fixes

| Problem | Fix |
|---|---|
| `BUILD FAILURE` — cannot resolve `spring-ai` | The Spring Milestones repo is in `pom.xml` — run `mvn clean package` once connected to internet |
| `401 Unauthorized` from GitHub | GitHub token expired or missing `Actions: Read` permission — regenerate it |
| `401` from OpenAI | Wrong API key in `.env` — check for extra spaces or quotes |
| `WebClient buffer limit exceeded` | Already handled — 16MB buffer is set in `AppConfig` |
| `.env` showing in `git status` | Run `git rm --cached .env` then commit |







POST /api/v1/analyze
Body: { "repo_owner": "paridhi-garg", "repo_name": "my-spring-app" }

  AnalysisController.analyze()
    → validates "paridhi-garg" matches [a-zA-Z0-9_.-]+  ✅
    → calls rcaService.analyze("paridhi-garg", "my-spring-app", null, "main")

  RCAService.analyze()
    → runId is null → calls getLatestFailedRun()

  GitHubService.getLatestFailedRun()
    → GET https://api.github.com/repos/paridhi-garg/my-spring-app/actions/runs
           ?branch=main&status=failure&per_page=1
    → Response: { "workflow_runs": [{ "id": 14523891, "name": "CI", "run_number": 47 ... }] }
    → Returns WorkflowRun(id=14523891, name="CI", runNumber=47, conclusion="failure")

  GitHubService.getJobsForRun(runId=14523891)
    → GET /repos/paridhi-garg/my-spring-app/actions/runs/14523891/jobs
    → Returns [Job(id=98765, name="build-and-test", conclusion="failure",
                   steps=[Step("Checkout", "success"), Step("Run tests", "failure")])]

  RCAService: failedJobNames = ["build-and-test"]

  GitHubService.getFailedJobLogs(runId=14523891)
    → getJobLogs(jobId=98765)
        → GET /repos/paridhi-garg/my-spring-app/actions/jobs/98765/logs
        → GitHub responds: HTTP 302
          Location: https://objects.githubusercontent.com/logs/98765?X-Amz-Signature=xyz
        → response.releaseBody() → discards empty 302 body
        → plainWebClient.get("https://objects.githubusercontent.com/...") 
        → Returns 2.3MB of raw log text
    → Truncate to last 8,000 chars  
    → Build: "=== JOB: build-and-test [failure] ===\n  FAILED STEP: Run tests\n<logs>"

  RCAService: cap total logs to 15,000 chars

  RCAService.callAI()
    → Builds prompt with run metadata + truncated logs
    → chatClient.prompt().user(prompt).call().content()
    → OpenAI API receives ~3,500 tokens
    → Returns:
      {
        "root_cause": "NullPointerException in UserServiceTest because UserRepository mock is not initialised.",
        "failed_steps": ["Run tests"],
        "recommendation": "Add @MockBean for UserRepository in UserServiceTest and initialise it in @BeforeEach.",
        "severity": "HIGH",
        "summary": "The CI pipeline failed in the test phase. A missing mock setup caused a NullPointerException in the user service tests. Add the missing @MockBean annotation and re-run."
      }

  RCAService.buildResponse()
    → Strips any markdown fences
    → objectMapper.readValue() → Map
    → Builds AnalysisResponse via @Builder

  AnalysisController
    → ResponseEntity.ok(response)
    → Jackson serializes to JSON
    → HTTP 200 returned to caller