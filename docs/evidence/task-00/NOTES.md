# Task 00 — Capture framework validation

Not one of the 15 assessed tasks. These two artefacts exist to prove the evidence
tooling itself works before it is relied upon for Tasks 1-15.

| File | What it proves |
|---|---|
| `framework-smoke-test.png` + `.log` | `run-and-shot.sh` executes a real command, captures real output with its ANSI colours, records the true exit code, and keeps the raw log alongside the image |
| `framework-web-selftest.png` | `shot-web.js` loads a live HTTP server in headless Chromium, performs scripted UI steps (fill `#username` → click submit → wait for `[data-testid=dashboard]`), and frames the result with the real URL |

The web self-test was additionally checked against an unreachable URL: the script
exited non-zero rather than emitting an empty image, which is what guarantees a
broken screenshot cannot pass silently.
