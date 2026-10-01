# GitHub UI evidence

Screenshots of the **real GitHub web interface**, captured with headless Chromium
against github.com.

| File | Shows |
|---|---|
| `01-repository-home.png` | Repository home with the README rendered |
| `02-release-v1.0.0.png` | **Release tag `v1.0.0` → `c0948c6`** |
| `03-pull-requests.png` | All three pull requests, merged |
| `04-issues.png` | The six backlog issues, **all closed**, with epic/priority/points labels |
| `05-pr7-review.png` | **PR #7 and the review** that raised two blocking findings |
| `06-pr9-conflict.png` | PR #9 — the conflict that was resolved before merge |
| `07-commit-history.png` | Commit history: Conventional Commits and merge commits |
| `08-branches.png` | The integration branch and three feature branches |

## Why these arrived late

For most of the project the build environment's network policy blocked
`github.githubassets.com`, which serves **all** of GitHub's CSS and JavaScript.
Pages therefore rendered as unstyled HTML, which is worthless as evidence, so
GitHub-side facts were taken from the **REST API** instead — see
[`../task-15/02-github-state.log`](../task-15/02-github-state.log).

Once the domain was allowed, two further problems had to be solved before these
captures worked, both recorded because they are easy to misdiagnose:

1. **`ERR_CERT_AUTHORITY_INVALID`.** The outbound proxy terminates TLS with its
   own CA, which Chromium does not trust. Context-level `ignoreHTTPSErrors` is
   **not** enough — the main-frame navigation still fails. The browser must be
   launched with `--ignore-certificate-errors`.
2. **Navigation hanging at `domcontentloaded`.** `collector.github.com`
   (GitHub telemetry) is still denied, and the proxy holds that connection open
   rather than refusing it, so the page never settles. `shot-web.js` gained a
   `--block` option that aborts such requests at the request layer.

Throughout the blocked period, no imitation of the GitHub interface was ever
produced. A styled lookalike built from API data and presented as a GitHub
screenshot would have been a fabrication, and a single fabricated image
invalidates an entire evidence pack.
