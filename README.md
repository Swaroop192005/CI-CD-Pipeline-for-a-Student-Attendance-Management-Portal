# Student Attendance Management Portal — CI/CD Pipeline

[![Java](https://img.shields.io/badge/Java-17-007396)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-6DB33F)](https://spring.io/projects/spring-boot)
[![Build](https://img.shields.io/badge/build-Maven%203.9-C71A36)](https://maven.apache.org/)
[![CI](https://img.shields.io/badge/CI-Jenkins-D24939)](https://www.jenkins.io/)
[![Tests](https://img.shields.io/badge/UI%20tests-Selenium%204-43B02A)](https://www.selenium.dev/)
[![Container](https://img.shields.io/badge/container-Docker-2496ED)](https://www.docker.com/)
[![Provisioning](https://img.shields.io/badge/provisioning-Ansible-EE0000)](https://www.ansible.com/)

A web portal that replaces paper attendance registers with a single system where
attendance is captured once, moves through an explicit **role-based approval
workflow**, and is visible to students in real time — delivered through a
complete DevOps toolchain from Git commit to a provisioned, running container.

> **The application is deliberately modest; the delivery pipeline around it is the
> subject of this project.** Every commit is built, tested behind a Selenium
> quality gate, packaged into a versioned container and deployed onto an
> Ansible-provisioned node, with no manual step in between.

---

## Why this exists

Attendance is still recorded on paper and merged into spreadsheets weeks later.
Students learn of a shortage against the statutory 75% threshold far too late to
act on it, drafts are indistinguishable from verified records, and edits leave no
audit trail. The full problem analysis — stakeholders, 12 catalogued pain points
and 18 measurable success criteria — is in
[Task 1](docs/01-problem-definition-and-scope.md).

## What it does

| Capability | Who | Story |
|---|---|---|
| Record attendance for a course session in one screen | FACULTY | US-06 |
| View, update and **search** records by student, course, date range and state | FACULTY, ADMIN | US-07, US-08, US-09 |
| Submit for approval; **approve or reject with a remark** | FACULTY submits, **ADMIN decides** | US-11 … US-13 |
| See own attendance percentage, flagged below threshold | STUDENT | US-10 |
| Summary dashboard with per-course percentages and pending queue | ADMIN | US-15, US-16 |

The workflow — and the separation of duties it enforces — is the heart of the
application:

```
DRAFT ──submit(FACULTY)──> SUBMITTED ──approve(ADMIN)──> APPROVED  [terminal]
                               │
                               └──reject(ADMIN)──> REJECTED ──revise(FACULTY)──> DRAFT
```

Only `APPROVED` records count towards the official percentage. A transition
outside this machine is refused by the service layer, not merely hidden in the UI.

---

## Technology

| Concern | Choice |
|---|---|
| Language / framework | Java 17, Spring Boot 3.3.13 (MVC, Thymeleaf, Data JPA, Security, Validation, Actuator) |
| Build | Apache Maven 3.9 — `war` packaging, embedded Tomcat scoped `provided` |
| Database | H2 file mode behind an externalised JDBC URL |
| App server | Apache Tomcat 10.1 (embedded for dev, standalone for deploy) |
| CI/CD | Jenkins — freestyle job and a declarative `Jenkinsfile` |
| Tests | JUnit 5 + Spring Boot Test; Selenium WebDriver 4 on headless Chromium |
| Containers | Docker, versioned images to a registry |
| Provisioning | Ansible — inventory, playbook, idempotency, rollback |

Server-rendered Thymeleaf is a deliberate choice over a JavaScript SPA: the
Selenium gate is central to this project, and synchronous DOM keeps those tests
stable rather than flaky.

---

## Quick start

**Requirements:** JDK 17+, Maven 3.9+. (Docker and Ansible are needed only from
Task 11 onward.)

```bash
git clone https://github.com/Swaroop192005/CI-CD-Pipeline-for-a-Student-Attendance-Management-Portal.git
cd CI-CD-Pipeline-for-a-Student-Attendance-Management-Portal

mvn -B clean package            # compile, run unit tests, build the WAR
scripts/app-control.sh start    # start and wait for health (≈8s)
```

Then open <http://localhost:8080>. Health probe: <http://localhost:8080/actuator/health>.

```bash
scripts/app-control.sh status   # is it up?
scripts/app-control.sh stop     # stop it
```

### Running the WAR directly

```bash
java -jar target/attendance-portal.war
```

The same artefact also deploys into a standalone Tomcat 10.1 `webapps/` directory
— that is why the embedded container is scoped `provided`.

### Configuration

Nothing environment-specific is hard-coded; every value below is an environment
variable with a local default.

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:h2:file:./data/sampdb` | JDBC URL — swap for PostgreSQL with no rebuild |
| `DB_USERNAME` / `DB_PASSWORD` | `sa` / *(empty)* | Credentials |
| `ATTENDANCE_THRESHOLD` | `75` | Minimum attendance percentage |
| `LOG_LEVEL` | `INFO` | Root log level |

```bash
ATTENDANCE_THRESHOLD=80 SERVER_PORT=9090 scripts/app-control.sh start --port 9090
```

---

## Project structure

```
.
├── pom.xml                        Maven build (Spring Boot 3.3.13, Java 17, war)
├── Jenkinsfile                    pipeline as code                        (Task 8)
├── Dockerfile                     container image                         (Task 11)
├── ansible/                       inventory, playbook, roles              (Task 13)
├── src/main/java/com/samp/attendance/
│   ├── config/                    security filter chain, role rules
│   ├── web/                       controllers
│   ├── service/                   business rules + workflow state machine
│   ├── repository/                Spring Data JPA
│   └── domain/                    entities and enums
├── src/main/resources/
│   ├── application.properties     externalised configuration
│   └── templates/                 Thymeleaf views
├── src/test/java/com/samp/attendance/
│   └── selenium/                  UI journeys, the quality gate           (Task 9)
├── scripts/
│   ├── app-control.sh             start / stop / wait for the app
│   └── capture/                   evidence capture tooling
└── docs/
    ├── 00-PROJECT-PLAN.md         master tracker and frozen stack
    ├── 01..15-*.md                one document per task
    ├── diagrams/                  Mermaid sources + rendered SVG
    └── evidence/                  screenshots and raw logs
```

---

## Documentation

| Task | Document |
|---|---|
| — | **[Master plan and progress tracker](docs/00-PROJECT-PLAN.md)** |
| 1 | [Problem Definition and Scope](docs/01-problem-definition-and-scope.md) |
| 2 | [Agile Planning and DevOps Workflow](docs/02-agile-planning.md) |
| 3 | [Requirements, Architecture and Technology Setup](docs/03-architecture.md) |
| 4 | [Git and GitHub Repository Initialization](docs/04-git-repository.md) |
| 5 | [Feature Development with Branching](docs/05-feature-development.md) |
| — | [Contributing: branch policy, commits, review](CONTRIBUTING.md) |
| — | [Evidence pack: rules and index](docs/evidence/README.md) |

---

## Evidence

Every task leaves **screenshot proof** in [`docs/evidence/`](docs/evidence/README.md),
produced by the committed tooling in [`scripts/capture/`](scripts/capture/):

* **`run-and-shot.sh`** executes a command for real, keeps the raw `.log`, and
  renders the transcript to PNG with the true exit code.
* **`shot-web.js`** drives headless Chromium against a genuinely running server
  and frames the capture with the live URL.

Nothing is mocked or edited, failures are captured as well as successes, and
every terminal screenshot ships with the log it was rendered from.

---

## Contributing

Branch naming, commit conventions and the review process are in
[CONTRIBUTING.md](CONTRIBUTING.md). In short: branch from `develop` as
`feature/US-06-short-description`, use Conventional Commits, open a PR, and never
merge a red build.

## Status

Task 6 of 15 complete — the MVP is functionally complete: attendance capture,
search, the role-based approval workflow, the summary dashboard and the admin
master-data screens all work end to end.
