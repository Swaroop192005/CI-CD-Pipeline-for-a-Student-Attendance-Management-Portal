# Evidence Pack — screenshots and raw logs

Every task in this project produces **visual proof** that the step was actually
performed. This directory holds that proof, one folder per task.

```
docs/evidence/
├── task-00/   capture-framework validation (this tooling proving itself)
├── task-01/   ...
└── task-NN/   <slug>.png  +  <slug>.log
```

## How the evidence is produced

There are two capture tools, both committed in [`scripts/capture/`](../../scripts/capture/).

### 1. Terminal evidence — `run-and-shot.sh`

```bash
./scripts/capture/run-and-shot.sh task-07 maven-build mvn -B clean package
```

This **actually executes the command**, records the real combined stdout/stderr
to `docs/evidence/task-07/maven-build.log`, and then typesets that transcript
into `docs/evidence/task-07/maven-build.png`.

### 2. Browser evidence — `shot-web.js`

```bash
node scripts/capture/shot-web.js --url http://localhost:8080/dashboard \
     --out docs/evidence/task-06/dashboard.png --title "Summary dashboard" \
     --steps login-faculty.json --wait-for "[data-testid=dashboard]"
```

This drives **headless Chromium against a genuinely running server**, optionally
performing a login or other UI steps first, then frames the capture in browser
chrome showing the live URL and capture time. If the URL does not respond, or a
step fails, the script exits non-zero — so a missing or broken screenshot can
never pass silently.

## Integrity rules

These rules are what make the pack worth anything at a viva:

1. **No screenshot is ever hand-drawn, mocked or edited.** Both tools render only
   what a real command printed or a real server served.
2. **Every terminal screenshot ships with its raw `.log`** next to it. The PNG is
   a typeset view of that file; anyone can diff the two. The `.log` is the
   primary record, the `.png` is for the report.
3. **Failures are kept, not hidden.** Tasks 10 and 14 deliberately require a red
   pipeline and a rollback. Those screenshots show genuine failure output, with
   the non-zero exit code in the footer, because "the gate works" is only
   provable by showing it block something.
4. **Timestamps are execution time**, not render time, so the ordering of
   evidence across tasks is real.
5. The terminal renderer may elide the middle of very long transcripts in the
   *image* (it marks the elision explicitly). The `.log` always holds the
   complete output.

## What each image shows

| Element | Meaning |
|---|---|
| Title bar (terminal) | the exact command that was run |
| Footer `✓ exit 0` / `✗ exit N` | the real process exit code |
| Footer timestamp | when the command actually executed (UTC) |
| URL bar (browser) | the live URL that was loaded, after any redirects |

## Index

| Task | Evidence | Notes |
|---|---|---|
| 00 | `framework-smoke-test.png` / `.log` | Terminal capture proving itself on a real `git log` |
| 00 | `framework-web-selftest.png` | Browser capture proving step automation (type → click → wait) against a live local server |
| 01 | `git-commit-evidence.png` / `.log` | The Task 1 baseline commit, author, date and message |
| 01 | `deliverable-structure.png` / `.log` | Documents produced by Task 1 and their sizes |
| 02 | `devops-lifecycle.png` | DevOps lifecycle Plan→Monitor with the tool used at each stage, rendered from `docs/diagrams/devops-lifecycle.mmd` |
| 02 | `kanban-board.png` | Board state at end of Task 2, showing WIP limits and the 24 backlog stories |
| 02 | `git-commit-evidence.png` / `.log` | The Task 2 commit |
| 03 | `maven-build.png` / `.log` | Real `mvn -B clean package` — BUILD SUCCESS, 4/4 tests |
| 03 | `build-artefact.png` / `.log` | The produced `attendance-portal.war` and its contents |
| 03 | `app-landing-page.png` | **Live app** at `localhost:8080`, showing config resolved at runtime |
| 03 | `actuator-health.png` | **Live** `/actuator/health` returning `UP` without authentication |
| 03 | `health-and-config.png` / `.log` | Health JSON and the externalised settings in effect |
| 03 | `use-case.png`, `architecture.png`, `er-model.png`, `workflow-state-machine.png` | Design diagrams rendered from committed `.mmd` sources |
| 03 | `git-commit-evidence.png` / `.log` | The Task 3 commit |
| 04 | `github-issues.png` / `.log` | The 6 issues raised from the backlog, read from the GitHub REST API |
| 04 | `github-repo-metadata.png` / `.log` | Repository visibility, default branch and remote branches |
| 04 | `git-log-graph.png` / `.log` | Commit history showing the Conventional Commits convention in use |
| 04 | `repo-structure.png` / `.log` | Tracked files after repository initialisation |
| 04 | `branch-and-remote.png` / `.log` | Branch tracking, remote and `.gitignore` rule count |
| 04 | `git-commit-evidence.png` / `.log` | The Task 4 commit |
| 05 | `01-login-page.png` … `07-saved-records-fixed.png` | **Live app**: login, roster defaulting to PRESENT, future-date refusal, 8 saved records |
| 05 | `06-defect-lazy-init.png` / `.log` | **A genuine HTTP 500** — LazyInitializationException on the records list, kept as required by rule 3 |
| 05 | `08-branch-merge-graph.png` / `.log` | Feature branch, two commits and the merge commit |
| 05 | `09-pull-request.png` / `.log` | PR #7 state, the review, and the issues it closed |
| 05 | `10-test-run.png` / `.log` | 17 tests passing |
| 06 | `01-two-sessions-recorded.png` … `06-search-empty-state.png` | **Live app**: 16 records, dashboard with percentages and SHORTAGE flags, approve/reject, combined search, student view, empty state |
| 06 | `07-merge-conflict.png` / `.log` | **A genuine merge conflict** with both sides' markers |
| 06 | `08-conflict-resolved.png` / `.log` | The resolution and the resulting history |
| 06 | `09-release-tag.png` / `.log` | Annotated tag `v1.0.0`, **including the HTTP 403 that blocks pushing it** |
| 06 | `10-test-run.png` / `.log` | 37 tests passing |
| 06 | `11-branch-graph.png` / `.log` | Three feature branches and three merges |

*(the table is extended as each task lands)*

## GitHub's web UI — captured, after the domain was allowed

For most of the project `github.githubassets.com` (all of GitHub's CSS and
JavaScript) was blocked by the network policy, so GitHub pages rendered as
unstyled HTML. Those captures were discarded rather than committed, and
GitHub-side facts were evidenced from the **REST API** instead — see
[`task-15/02-github-state.log`](task-15/02-github-state.log).

Once the domain was allowed, the real interface was captured:
**[`github-ui/`](github-ui/README.md)** — repository home, the `v1.0.0` release,
all three merged pull requests, the review on #7, the resolved conflict on #9,
all six closed issues, the commit history and the branches.

Two further problems had to be solved first, both recorded in that folder's
README: the proxy's TLS interception needed `--ignore-certificate-errors` at
browser launch (context-level `ignoreHTTPSErrors` is not sufficient for the
main-frame navigation), and a still-blocked telemetry host held connections open
until the page timed out, which `shot-web.js --block` now aborts.

**At no point was an imitation of the GitHub interface produced.** A styled
lookalike built from API data and presented as a screenshot would be a
fabrication, and one fabricated image invalidates an entire evidence pack.

## A note on the two rendered diagrams

`devops-lifecycle.png` and `kanban-board.png` are **renders of committed source**,
not captures of a running system — there is no system to run yet at Task 2. Their
sources are `docs/diagrams/devops-lifecycle.mmd` (also rendered natively by GitHub)
and `docs/diagrams/kanban-board.html`, both committed, so each image can be
regenerated and checked against its source. From Task 5 onward the browser
evidence is captured from a genuinely running server instead.
