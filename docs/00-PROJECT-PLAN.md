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

## Task status

| # | Task | Status | Deliverable location |
|---|---|---|---|
| 1 | Problem Definition and Scope | ✅ Done | `docs/01-problem-definition-and-scope.md` |
| 2 | Agile Planning and DevOps Workflow | ⬜ Pending | `docs/02-agile-planning.md` |
| 3 | Requirements, Architecture and Technology Setup | ⬜ Pending | `docs/03-architecture.md` |
| 4 | Git and GitHub Repository Initialization | ⬜ Pending | `README.md`, `.gitignore`, `.github/` |
| 5 | Feature Development with Branching | ⬜ Pending | feature branch + PR |
| 6 | MVP Completion and Git Collaboration | ⬜ Pending | tagged release `v1.0.0` |
| 7 | Jenkins Installation and CI Job | ⬜ Pending | `docs/07-jenkins-ci.md`, `jenkins/` |
| 8 | Pipeline as Code and Server Deployment | ⬜ Pending | `Jenkinsfile` |
| 9 | Selenium Test Design and Local Execution | ⬜ Pending | `src/test/java/.../selenium/` |
| 10 | Continuous Testing in Jenkins | ⬜ Pending | `docs/10-continuous-testing.md` |
| 11 | Docker Image and Container Lifecycle | ⬜ Pending | `Dockerfile`, `docs/11-docker.md` |
| 12 | Jenkins-Docker Continuous Deployment | ⬜ Pending | `Jenkinsfile` (CD stages) |
| 13 | Configuration Management Script | ⬜ Pending | `ansible/` |
| 14 | Automated Provisioning and Reliability Validation | ⬜ Pending | `docs/14-provisioning.md` |
| 15 | Final End-to-End Release, Documentation and Viva | ⬜ Pending | `docs/15-final-report.md` |

Legend: ⬜ Pending · 🟨 In progress · ✅ Done
