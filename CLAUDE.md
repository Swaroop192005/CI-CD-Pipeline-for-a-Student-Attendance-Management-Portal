# Student Attendance Management Portal — context for Claude Code

A college attendance portal delivered through a full DevOps toolchain. The
application is deliberately modest; **the CI/CD pipeline around it is the point of
the project**. Delivered in 15 tasks, all complete.

## Orientation — read these first

| File | Why |
|---|---|
| `docs/00-PROJECT-PLAN.md` | Master tracker, frozen technology stack, all 15 tasks |
| `docs/15-final-report.md` | End-to-end run, **troubleshooting guide**, limitations, viva Q&A |
| `docs/03-architecture.md` | SRS, diagrams, data model, endpoints, **port map** |
| `CONTRIBUTING.md` | Branch policy and commit conventions |
| `docs/evidence/README.md` | Evidence rules — how screenshots are produced and why they are trustworthy |

## Running it

```bash
scripts/demo-up.sh            # build + 37 unit tests + app on :8080
scripts/demo-up.sh --full     # + Jenkins :8090, Selenium Grid :4444, registry :5000, Tomcat :8082
scripts/demo-up.sh --down     # stop everything

mvn -B clean package          # 37 unit tests
mvn -Pselenium verify         # + 6 Selenium journeys (43 total)
scripts/app-control.sh start|stop|status
scripts/jenkins-build.sh samp-pipeline
cd ansible && ansible-playbook site.yml -e app_version=latest
```

Demo logins: `faculty1/faculty123` · `admin/admin123` · `22cs001/student123`

Ports: 8080 dev · 8081 container · 8082 Tomcat · 8083 Ansible node · 8090 Jenkins ·
5000 registry · 4444 Selenium Grid.

## Rules this codebase holds to

1. **Workflow transitions are refused in the service layer**, never merely hidden
   in the UI. `WorkflowState` owns the permitted transitions; `APPROVED` is
   terminal by having an empty transition set. A disabled button is not access
   control (AC-14.1).
2. **Percentages count `APPROVED` records only.** A rejected record is excluded
   from the denominator rather than counted as an absence — it is "not yet
   established", not "absent". A student with no approved records shows `—`, never
   `0%`. This is the most misread part of the product; do not "fix" it.
3. **No `Thread.sleep` in Selenium tests**; explicit waits only. Selectors are
   `data-testid`, never CSS classes or text position.
4. **Never skip, disable or quarantine a test to get a green build.**
5. **Nothing environment-specific is hard-coded** — port, datasource and the
   attendance threshold are environment variables with local defaults.
6. **Evidence is never mocked.** Screenshots come from real runs, every terminal
   image ships with the `.log` it was rendered from, and failures are kept.

## Things that will bite you

These were all hit during the build; the full list is in `docs/15-final-report.md` §5.

- `spring.jpa.open-in-view=false` — a view touching a lazy proxy throws
  `LazyInitializationException`. Service tests run inside `@Transactional` and
  will **not** catch it; `AttendanceListRenderingTest` renders outside one.
- H2 2.x rejects `AUTO_SERVER=TRUE` combined with `DB_CLOSE_ON_EXIT=FALSE`.
- Spring Security accepts logout only as **POST**; `GET /logout` silently does
  nothing and leaves you signed in as the previous user.
- Jenkins learns a pipeline's parameters only by *running* the Jenkinsfile once,
  so newly added parameters are empty on the run that introduces them.
- A parameterised Jenkins job must be triggered via `buildWithParameters`.
- Tomcat creates its exploded webapp directory as root:750 — do not try to delete
  it from the build; Tomcat replaces it when it sees a newer WAR.

## Environment note

This repository was built in a cloud container with a restrictive network policy,
which shaped two things: the Jenkins image bakes in a plugin set copied from
another image (the Jenkins update sites were blocked), and Maven is bind-mounted
into it. **On a normal network, install Jenkins plugins the standard way** —
`docs/07-jenkins-ci.md` documents both paths.
