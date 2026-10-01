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

*(the table is extended as each task lands)*
