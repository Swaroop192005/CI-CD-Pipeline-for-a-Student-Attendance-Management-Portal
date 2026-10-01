# Contributing — branching, commits and review

This is the working agreement for the Student Attendance Management Portal. It is
deliberately short: every rule here exists because the CI/CD pipeline depends on it.

---

## 1. Branch policy

### 1.1 Long-lived branches

| Branch | Purpose | Rules |
|---|---|---|
| `main` | Release-ready. Every commit is a candidate for deployment. | No direct pushes. Merges from `develop` only, via PR. Tagged on release. |
| `develop` | Integration branch. All feature work lands here first. | No direct pushes. Merges from feature branches only, via PR. Jenkins builds every commit. |

### 1.2 Short-lived branches

Branch from `develop`, merge back to `develop` through a pull request, then delete.

```
<type>/<backlog-id>-<short-kebab-description>
```

| Type | Use for | Example |
|---|---|---|
| `feature/` | New user-facing capability | `feature/US-06-record-attendance` |
| `bugfix/` | Defect in `develop` | `bugfix/US-12-faculty-can-approve` |
| `hotfix/` | Urgent defect in `main` — branch from `main`, merge to **both** `main` and `develop` | `hotfix/US-01-login-loop` |
| `ops/` | Pipeline, build, container or provisioning work | `ops/jenkins-pipeline-as-code` |
| `docs/` | Documentation only | `docs/task-15-final-report` |
| `release/` | Release stabilisation before tagging | `release/v1.0.0` |

**Rules**

1. Lower-case, hyphen-separated. No spaces, no underscores, no `#`.
2. The backlog ID (`US-06`) is mandatory for `feature/`, `bugfix/` and `hotfix/`.
3. One branch per story. If a branch needs two stories, it is two branches.
4. Rebase or merge `develop` into your branch before opening a PR — never the reverse.
5. Delete the branch after merge. The PR preserves the history.

### 1.3 Tags

Semantic versioning, annotated tags only:

```bash
git tag -a v1.0.0 -m "MVP: attendance capture, approval workflow, dashboard"
git push origin v1.0.0
```

`MAJOR.MINOR.PATCH` — MAJOR for a breaking data or API change, MINOR for a new
story, PATCH for a fix with no new behaviour.

---

## 2. Commit messages

[Conventional Commits](https://www.conventionalcommits.org/). The type prefix is
what lets the release notes be generated from the log.

```
<type>(<scope>): <imperative summary, <= 72 chars, no trailing period>

<body: what changed and WHY. Wrap at 80. The diff shows what;
the message must explain why.>

Refs: US-06
```

| Type | Use |
|---|---|
| `feat` | New capability |
| `fix` | Defect correction |
| `docs` | Documentation only |
| `test` | Adding or correcting tests |
| `build` | Build system, dependencies, packaging |
| `ci` | Jenkins, pipeline, container, provisioning |
| `refactor` | Behaviour-preserving restructuring |
| `chore` | Housekeeping |

Scope is the task or module: `task-05`, `workflow`, `dashboard`, `jenkins`.

**Not acceptable:** `update`, `fix bug`, `changes`, `asdf`, `final`, `final2`.
A reviewer must be able to understand the change from the message alone.

---

## 3. Pull requests

1. Open against `develop` (or `main` for a `hotfix/`).
2. Fill in the PR template — the "how it was verified" section must contain
   commands you actually ran and their real result.
3. Link the issue with `Closes #<n>` so it closes on merge.
4. The Jenkins build must be green. **A red build is never merged.**
5. Address every review comment: implement it, or reply explaining why not.
6. Merge with a merge commit (not squash), so the feature branch's history
   survives as evidence of the workflow.

---

## 4. Definition of Done

A change is not done until it meets all 14 conditions in
[`docs/02-agile-planning.md` §8](docs/02-agile-planning.md#8-definition-of-done).
In particular:

* The business rules it introduces are covered by tests.
* Screenshot evidence is captured under `docs/evidence/task-NN/`.
* **No test is ever skipped, disabled or quarantined to make a build green.**

---

## 5. Local checks before pushing

```bash
mvn -B clean package          # compile + unit tests + package
scripts/app-control.sh start  # verify it actually starts
scripts/app-control.sh stop
```

A green `mvn package` is necessary but not sufficient — Task 3 found a defect
that passed every test and still prevented the application from starting. If you
changed configuration, start the application before you push.
