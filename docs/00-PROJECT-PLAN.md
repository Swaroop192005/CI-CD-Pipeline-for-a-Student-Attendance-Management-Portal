# Project Master Plan & Progress Tracker

**Project:** CI/CD Pipeline for a Student Attendance Management Portal (SAMP)
**Owner:** Swaroop192005
**Repository:** https://github.com/Swaroop192005/CI-CD-Pipeline-for-a-Student-Attendance-Management-Portal
**Development branch:** `claude/determined-goldberg-ml3brm`

This file is the single source of truth for the 15-task delivery. Each task is
completed in sequence and committed separately so that the Git history itself is
evidence of the DevOps lifecycle.

---

## Frozen technology stack

These decisions are made once in Task 1/Task 3 and are **not revisited** in later
tasks, so that the pipeline built in Tasks 7-15 has a stable target.

| Concern | Decision | Why |
|---|---|---|
| Language | Java 17 (source/target), JDK 21 runtime | Available in the build environment; LTS |
| Framework | Spring Boot 3.3.x (Spring MVC, Thymeleaf, Spring Data JPA, Spring Security) | Server-rendered HTML is directly testable by Selenium |
| Build tool | Apache Maven 3.9 | Required by the brief; standard Jenkins integration |
| Packaging | `war` with embedded-Tomcat fallback | One artefact that runs via `java -jar` **and** deploys to a standalone Tomcat |
| Database | H2 (file mode) | Zero-install, survives restarts, swappable via JDBC properties |
| Web/App server | Apache Tomcat 10.1 (Jakarta EE 10) | Required by the brief |
| CI/CD engine | Jenkins (Declarative Pipeline, `Jenkinsfile`) | Required by the brief |
| Unit/integration tests | JUnit 5 + Spring Boot Test | Fast feedback before the Selenium gate |
| UI tests | Selenium WebDriver 4 + headless Chromium | Required by the brief |
| Containers | Docker (multi-stage build) + Docker Hub / local registry | Required by the brief |
| Configuration management | Ansible (inventory + playbook + roles) | Chosen over Puppet: agentless, YAML, easier to demo idempotency |
| Version control | Git + GitHub, trunk-based with short-lived feature branches | Required by the brief |

---

## Standing rule: visual evidence for every task

Every task must leave **screenshot proof** in `docs/evidence/task-NN/`, captured
by the committed tooling in `scripts/capture/`:

* `run-and-shot.sh` — runs a command for real, keeps the raw `.log`, renders the
  transcript to `.png`.
* `shot-web.js` — drives headless Chromium against a genuinely running server
  (app, dashboard, Jenkins UI) and frames the capture with the live URL.

Rules and the full index: [`docs/evidence/README.md`](evidence/README.md).
Screenshots are never mocked or edited, failures are captured as well as
successes, and every terminal image ships with the log it was rendered from.

---

## Task status

| # | Task | Status | Deliverable location | Evidence |
|---|---|---|---|---|
| 1 | Problem Definition and Scope | ✅ Done | `docs/01-problem-definition-and-scope.md` | `evidence/task-01/` |
| 2 | Agile Planning and DevOps Workflow | ✅ Done | `docs/02-agile-planning.md` | `evidence/task-02/` |
| 3 | Requirements, Architecture and Technology Setup | ✅ Done | `docs/03-architecture.md` | `evidence/task-03/` |
| 4 | Git and GitHub Repository Initialization | ✅ Done | `docs/04-git-repository.md`, `README.md`, `.github/` | `evidence/task-04/` |
| 5 | Feature Development with Branching | ✅ Done | `docs/05-feature-development.md`, PR #7 | `evidence/task-05/` |
| 6 | MVP Completion and Git Collaboration | ✅ Done | `docs/06-mvp-completion.md`, tag `v1.0.0` | `evidence/task-06/` |
| 7 | Jenkins Installation and CI Job | ⬜ Pending | `docs/07-jenkins-ci.md`, `jenkins/` | `evidence/task-07/` |
| 8 | Pipeline as Code and Server Deployment | ✅ Done | `Jenkinsfile`, `docs/08-pipeline-and-deployment.md` | `evidence/task-08/` |
| 9 | Selenium Test Design and Local Execution | ✅ Done | `docs/09-selenium-tests.md`, `src/test/java/.../selenium/` | `evidence/task-09/` |
| 10 | Continuous Testing in Jenkins | ✅ Done | `docs/10-continuous-testing.md` | `evidence/task-10/` |
| 11 | Docker Image and Container Lifecycle | ✅ Done | `Dockerfile`, `docs/11-docker.md` | `evidence/task-11/` |
| 12 | Jenkins-Docker Continuous Deployment | ✅ Done | `Jenkinsfile`, `docs/12-jenkins-docker-cd.md` | `evidence/task-12/` |
| 13 | Configuration Management Script | ✅ Done | `ansible/`, `docs/13-configuration-management.md` | `evidence/task-13/` |
| 14 | Automated Provisioning and Reliability Validation | ✅ Done | `docs/14-provisioning-and-reliability.md` | `evidence/task-14/` |
| 15 | Final End-to-End Release, Documentation and Viva | ✅ Done | `docs/15-final-report.md` | `evidence/task-15/` |

Legend: ⬜ Pending · 🟨 In progress · ✅ Done
