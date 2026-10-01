# Task 1 — Problem Definition and Scope

**Project:** CI/CD Pipeline for a Student Attendance Management Portal (SAMP)
**Document status:** Approved baseline (scope frozen)
**Version:** 1.0

---

## 1. Background and real-time need

Attendance in most Indian colleges and universities is still captured on paper
registers or in ad-hoc spreadsheets that are mailed around at the end of a term.
This creates a measurable, recurring problem:

* A faculty member spends 3-5 minutes per lecture marking a paper register and a
  further 2-3 hours per month transcribing those registers into a consolidated
  spreadsheet for the department.
* Students have no way to see their own attendance percentage until the
  consolidated sheet is published, which is typically **4-6 weeks** after the
  classes happened — far too late to act on a shortage.
* Most institutions enforce a statutory minimum attendance (commonly **75%**) as
  a precondition for sitting examinations. Because the data arrives late,
  "detained" lists are published days before exams, triggering disputes,
  manual re-verification and appeals.
* Corrections are untraceable. A register overwritten in ink, or a cell edited in
  a shared spreadsheet, leaves no record of *who* changed *what* and *when*.
* There is no separation of duties: the same spreadsheet that faculty edit is the
  one the Head of Department reports from, so an unverified draft is
  indistinguishable from an approved record.

The **Student Attendance Management Portal** replaces this with a single
web application where attendance is captured once, moves through an explicit
review workflow, and is visible to every stakeholder in real time.

### The DevOps need

The application itself is deliberately modest. The real subject of this project
is the **delivery pipeline around it**. A college portal of this kind is
maintained by a small team (often a single faculty coordinator and two student
assistants) across semesters, which is exactly the situation where manual
build-and-copy deployment fails:

* Builds are produced on one developer's laptop and cannot be reproduced.
* Nobody knows which commit is running on the server.
* A regression in the attendance-percentage calculation reaches production
  because nothing ran the tests.
* Re-provisioning the server after a crash is tribal knowledge.

This project therefore builds an **automated path from a Git commit to a running,
tested, containerised deployment**, using Git/GitHub, Jenkins, Maven, Selenium,
Docker and Ansible.

---

## 2. Problem statement

> Faculty, students and department heads at a college currently depend on paper
> registers and manually merged spreadsheets to record and consume attendance.
> The process is slow (weeks of lag before a student learns of an attendance
> shortage), error-prone (manual transcription, silent edits, no audit trail),
> and offers no separation between a draft record and an approved one. In
> parallel, the small team maintaining any replacement software has no repeatable
> way to build, test, deploy or re-provision it, so changes are risky and
> infrequent.
>
> **This project delivers (a) a minimal web portal that captures attendance once,
> moves each record through an explicit role-based approval workflow, and exposes
> a live summary dashboard; and (b) a fully automated CI/CD pipeline that takes
> every commit through build, automated UI testing, containerisation and
> deployment onto a server provisioned by configuration-management code.**

---

## 3. Target users and stakeholders

### 3.1 Primary users (interact with the system daily/weekly)

| User | Role in system | What they do | Primary need |
|---|---|---|---|
| **Faculty / Lecturer** | `FACULTY` | Creates and edits attendance records for the sessions they teach; submits them for approval | Mark a full class in under a minute; correct mistakes before submitting |
| **Head of Department / Coordinator** | `ADMIN` | Reviews submitted records, approves or rejects them; manages students and courses; views the department dashboard | Trust that approved data is final and attributable |
| **Student** | `STUDENT` | Views their own attendance record and percentage per course | Know *today* whether they are below the 75% threshold |

### 3.2 Secondary stakeholders (affected by, but do not operate, the system)

| Stakeholder | Interest / stake |
|---|---|
| **Examination cell** | Consumes the approved attendance percentage to publish eligibility lists |
| **Parents / guardians** | Receive the attendance standing of the student (via the student's view) |
| **College IT / system administrator** | Owns the server, the Tomcat/Docker runtime and backups; needs reproducible provisioning |
| **DevOps / build engineer (the project author)** | Owns the Jenkins pipeline, the Docker images and the Ansible playbooks |
| **Accreditation & audit bodies (NAAC/NBA)** | Require demonstrable, tamper-evident attendance records |
| **University administration** | Requires the statutory minimum-attendance policy to be enforced consistently |

### 3.3 Stakeholder influence map

| | **Low interest** | **High interest** |
|---|---|---|
| **High influence** | University administration | HoD / Coordinator, College IT |
| **Low influence** | Accreditation bodies | Faculty, Students, Parents |

---

## 4. Existing pain points (the "as-is" problems)

| # | Pain point | Current cost | Addressed by |
|---|---|---|---|
| P1 | Attendance recorded on paper, transcribed later | ~2-3 hrs/month per faculty member | Direct capture in the portal (Task 5) |
| P2 | Students learn of a shortage 4-6 weeks late | Detention disputes, no time to recover | Student dashboard, live percentage (Task 6) |
| P3 | No distinction between draft and verified data | HoD reports from unverified numbers | DRAFT → SUBMITTED → APPROVED workflow (Task 6) |
| P4 | Silent edits, no audit trail | Disputes cannot be settled | Immutable status transitions + `updatedBy`/`updatedAt` (Task 6) |
| P5 | Finding one student's record means scanning registers | Minutes per query | Search by student / course / date / status (Task 6) |
| P6 | Anyone with the spreadsheet can change anything | No separation of duties | Role-based access control (Task 5/6) |
| P7 | Builds are not reproducible | "Works on my machine" | Maven build on Jenkins (Task 7) |
| P8 | No automated regression testing | Defects reach users | Selenium quality gate (Tasks 9-10) |
| P9 | Manual, undocumented deployment | Unknown version in production | Jenkinsfile deploy stage (Task 8) |
| P10 | Environment drift between machines | "It ran on the old server" | Docker image (Task 11) + Ansible (Task 13) |
| P11 | Server rebuild is tribal knowledge | Hours of downtime after a failure | Idempotent Ansible provisioning (Task 14) |
| P12 | No way back from a bad release | Prolonged outage | Versioned images + rollback procedure (Task 14) |

---

## 5. Objectives

### 5.1 Product objectives (the portal)

* **O1** — Allow a faculty member to record attendance for a course session in a
  single screen, with create, view, update and search over the records.
* **O2** — Enforce a role-based status workflow so that only an `ADMIN` can move a
  record to its final approved state.
* **O3** — Present a summary dashboard showing attendance percentage per student
  per course, and highlight students below the configured threshold.
* **O4** — Restrict every page by role, so a `STUDENT` can only ever see their own data.

### 5.2 DevOps objectives (the pipeline — the main assessment target)

* **O5** — Keep the entire source, build, test, container and infrastructure
  definition in one Git repository with a documented branching policy.
* **O6** — Produce a versioned, archived build artefact on every commit, with no
  manual steps.
* **O7** — Express the whole pipeline as code (`Jenkinsfile`) under version control.
* **O8** — Gate deployment on an automated Selenium suite: **failing tests must stop
  the deployment**.
* **O9** — Package the application as a versioned Docker image, published to a
  registry, and deploy a fresh container automatically after the tests pass.
* **O10** — Provision the target environment from an Ansible playbook that is
  **idempotent**, and demonstrate health-checking and rollback to the previous
  stable release.

---

## 6. Constraints

### 6.1 Technical constraints

| # | Constraint | Consequence on design |
|---|---|---|
| C1 | Build tool must be Maven/Gradle/Ant | **Maven** chosen |
| C2 | Deployment target must be Tomcat or Nginx | **Tomcat 10.1**; `war` packaging |
| C3 | Configuration management must be Puppet or Ansible | **Ansible** chosen |
| C4 | CI server must be Jenkins | Declarative pipeline, not GitHub Actions |
| C5 | UI tests must use Selenium WebDriver | Server-rendered Thymeleaf UI with stable `data-testid` hooks |
| C6 | Single-node, low-resource environment | H2 file database instead of a separate DB server; no Kubernetes |
| C7 | Browser automation must run without a display | Headless Chromium |
| C8 | Java 17 language level | No preview features |

### 6.2 Project constraints

| # | Constraint |
|---|---|
| C9 | Scope is fixed at exactly **15 tasks**; anything outside the MVP list in §8 is deferred |
| C10 | Delivered by a single student developer; no parallel team, so collaboration (PRs, conflicts, reviews) is demonstrated on feature branches |
| C11 | Each task must produce its own committed, reviewable evidence |
| C12 | No paid/licensed software; open-source and free tiers only |
| C13 | No real student personal data — seeded demo data only |

### 6.3 Assumptions

* A1 — Users access the portal over the college LAN; public internet exposure and TLS termination are out of scope for the MVP.
* A2 — One academic term is in scope; historical term archiving is deferred.
* A3 — Authentication is local (username/password seeded by the application); no LDAP/SSO.
* A4 — The minimum attendance threshold (default 75%) is a configurable parameter, not a hard-coded rule.

---

## 7. Measurable success criteria

A criterion only counts if it can be evidenced from an artefact in this repository.

### 7.1 Product criteria

| ID | Criterion | Target | Evidence |
|---|---|---|---|
| S1 | Marking attendance for one session | ≤ 60 seconds, ≤ 2 page loads | Selenium journey timing (Task 9) |
| S2 | Student sees current attendance % | Immediately on login, 0 days lag | Dashboard screenshot (Task 6) |
| S3 | Records below threshold are flagged | 100% of students under 75% flagged | Dashboard test assertion (Task 9) |
| S4 | Unauthorised role access | 0 successful cross-role accesses | Security test (Task 6/9) |
| S5 | Search returns a known record | ≤ 1 second on the seeded dataset | Selenium journey (Task 9) |
| S6 | Status workflow integrity | 0 transitions allowed outside the defined state machine | Unit tests (Task 6) |

### 7.2 Pipeline criteria

| ID | Criterion | Target | Evidence |
|---|---|---|---|
| S7 | Commit-to-build trigger | Build starts automatically on push/poll | Jenkins trigger log (Task 7) |
| S8 | Build reproducibility | 3 consecutive builds of one commit produce an identical artefact name/version | Jenkins build log (Task 7) |
| S9 | Build duration | ≤ 5 minutes from commit to archived artefact | Jenkins build log (Task 7) |
| S10 | Pipeline as code | 100% of stages defined in `Jenkinsfile` | File in repository (Task 8) |
| S11 | Quality gate | A failing Selenium test leaves the deploy stage **not executed** | Failed-pipeline evidence (Task 10) |
| S12 | Defect correction loop | 1 deliberately injected defect caught, fixed, pipeline green again | Commit + reruns (Task 10) |
| S13 | Container start | Container serves HTTP 200 on `/actuator/health` within 60 s | Docker log (Task 11) |
| S14 | Image versioning | Every image tagged with the build number **and** `latest` | Registry listing (Task 12) |
| S15 | End-to-end automation | 0 manual steps between `git push` and a new running container | Pipeline run (Task 12) |
| S16 | Idempotency | 2nd Ansible run reports `changed=0` | Ansible output (Task 14) |
| S17 | Rollback | Previous stable version restored and healthy in ≤ 5 minutes | Rollback demo (Task 14) |
| S18 | Documentation completeness | All 15 tasks have a committed deliverable | `docs/00-PROJECT-PLAN.md` (Task 15) |

---

## 8. Approved MVP scope (frozen)

### 8.1 In scope

**Functional**

1. **Authentication & roles** — local login; three roles: `ADMIN`, `FACULTY`, `STUDENT`.
2. **Master data** — courses and students, seeded at startup; `ADMIN` may add.
3. **Create** — record attendance for a (course, session date, student) as
   `PRESENT` / `ABSENT` / `LATE`.
4. **View** — list attendance records with paging.
5. **Update** — edit an existing record while it is still editable for that role.
6. **Search** — filter records by student, course, date range and workflow status.
7. **Role-based status workflow** — `DRAFT → SUBMITTED → APPROVED | REJECTED`,
   with `REJECTED → DRAFT` for rework. `FACULTY` may submit; only `ADMIN` may
   approve or reject.
8. **Summary dashboard** — attendance percentage per student per course, counts by
   workflow status, and a below-threshold highlight.

**Non-functional**

9. Role-restricted pages (no horizontal or vertical privilege escalation).
10. Stable `data-testid` attributes on all elements that Selenium drives.
11. A `/actuator/health` endpoint for container and Ansible health checks.
12. Externalised configuration (port, database URL, attendance threshold) via
    environment variables / Spring profiles.

**DevOps**

13. Git repository with README, `.gitignore`, issue templates and a branch policy.
14. Jenkins freestyle CI job **and** a declarative `Jenkinsfile` pipeline.
15. Selenium suite of 3-5 critical journeys wired into the pipeline as a gate.
16. `Dockerfile`, versioned images, registry publication, automated container deploy.
17. Ansible inventory + playbook for provisioning, with idempotency, health check
    and rollback.

### 8.2 Explicitly out of scope (deferred)

| Deferred item | Reason |
|---|---|
| Biometric / RFID / face-recognition capture | Hardware dependency; outside a 15-task scope |
| Mobile native application | Responsive web is sufficient for the MVP |
| SMS/e-mail notification to parents | Needs a paid gateway (constraint C12) |
| LDAP / SSO / OAuth integration | Constraint A3 |
| Multi-term and multi-campus data partitioning | Constraint A2 |
| Timetable and leave-management modules | Not required by the MVP objectives |
| PDF/Excel report export | Dashboard satisfies O3 |
| Kubernetes orchestration, multi-node HA | Constraint C6 |
| Production-grade RDBMS (PostgreSQL/MySQL) migration | H2 satisfies the MVP; JDBC URL is externalised so a swap is a configuration change |
| Public internet exposure, TLS, WAF | Constraint A1 |

### 8.3 Scope approval

| Role | Name | Decision | Date |
|---|---|---|---|
| Product owner (student) | Swaroop | Approved | Task 1 |
| Reviewer (faculty guide) | — | Approved for MVP build | Task 1 |

**Change control:** any addition to §8.1 after this point requires a corresponding
removal, so that the task count stays at 15.

---

## 9. Risks identified at definition time

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Selenium tests are flaky in headless CI | High | High | Explicit waits only (no `Thread.sleep`), fixed seed data, stable `data-testid` selectors |
| R2 | Jenkins cannot be installed/persisted in the environment | Medium | High | Provide a reproducible Jenkins setup (Docker Compose + JCasC + job DSL) committed to the repo, so the configuration is evidence even where the daemon is ephemeral |
| R3 | Docker registry push requires credentials that cannot be shared | Medium | Medium | Support a **local registry** as the default target; Docker Hub via credentials binding |
| R4 | Scope creep beyond 15 tasks | Medium | Medium | Frozen scope §8 + change-control rule |
| R5 | Port conflicts between Tomcat, Jenkins, the registry and containers | Medium | Low | Port map fixed in Task 3 and parameterised in Task 8 |
| R6 | H2 file corruption losing demo data | Low | Low | Data re-seeded at startup; database file is disposable |

---

## 10. Task 1 deliverable checklist

- [x] Problem statement (§2)
- [x] Target users and stakeholder list (§3)
- [x] Existing pain points (§4)
- [x] Objectives (§5)
- [x] Constraints and assumptions (§6)
- [x] Measurable success criteria (§7)
- [x] Approved, frozen 15-task MVP scope (§8)
- [x] Initial risk register (§9)
