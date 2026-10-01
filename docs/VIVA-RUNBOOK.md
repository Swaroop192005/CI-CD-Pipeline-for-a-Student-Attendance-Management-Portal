# Viva Runbook — how to present this project

A step-by-step script for the day. Follow it in order. You do not need to explain
everything you built; you need to tell **one clear story** and prove it.

---

## The one sentence to open with

> "I built a student attendance portal, but the real subject of the project is the
> **CI/CD pipeline around it** — every commit is automatically built, tested behind
> a Selenium quality gate, packaged into a versioned container, and deployed to a
> server provisioned from code."

If you say nothing else well, say that. It frames everything that follows.

---

## Before the viva — the 20-minute preparation

Do this **the day before**, not on the day.

```bash
cd CI-CD-Pipeline-for-a-Student-Attendance-Management-Portal
git pull
scripts/demo-up.sh            # app + 37 tests on :8080   (~2 min)
```

Open <http://localhost:8080> and sign in as `faculty1` / `faculty123`. If that
works, **you have a live demo**. If it does not, do not panic — see §"If nothing
runs" below; your screenshots are a complete substitute.

Then open these five tabs and leave them open:

1. `http://localhost:8080` — the running app
2. `docs/evidence/task-10/01-gate-blocks-deploy.png` — **the key screenshot**
3. `docs/evidence/task-07/01-jenkins-job.png` — Jenkins
4. GitHub repo page — PRs, issues, the `v1.0.0` release
5. `docs/15-final-report.md` — your answer sheet

---

## The 10-minute presentation

### 1. The problem (1 min) — no screen needed

"Attendance is recorded on paper and merged into spreadsheets weeks later.
Students find out they are below the 75% requirement far too late to fix it.
Drafts and verified records look identical, and edits leave no audit trail."

### 2. The live app (3 min) — your strongest, lowest-risk segment

Sign in as **faculty1** and do exactly this:

| Do | Say |
|---|---|
| Mark attendance → select CS301 → Load roster | "One screen, the whole class, defaulting to PRESENT." |
| Set one student ABSENT → Save | "Saved as DRAFT. A faculty member records, but cannot approve." |
| Sign out, sign in as **admin** | "Separation of duties — this is the heart of it." |
| Approve the records | "Only an ADMIN can do this. The server refuses it for anyone else, not just the UI." |
| Open **Dashboard** | "Percentages, and the student below 75% flagged." |

**Then point at one number and explain it** — this is the line that shows you
designed the system rather than assembled it:

> "Notice this student shows **one** approved session, not two. Their other record
> was rejected, so it is excluded from the denominator entirely rather than counted
> as an absence. A rejected record isn't 'absent' — it's **'not yet established'**.
> An unverified record must never move the official figure in either direction."

### 3. The pipeline (3 min) — use the screenshots, do not run Jenkins live

Show `docs/evidence/task-07/01-jenkins-job.png` and `docs/evidence/task-08/`:

"Every commit triggers Jenkins. It builds with Maven, runs 37 unit tests, then 6
Selenium journeys, packages a WAR, builds a versioned Docker image, publishes it
to a registry and deploys a fresh container — then Ansible provisions the server
from code. **No manual step anywhere.**"

### 4. The moment that wins it (2 min) — `01-gate-blocks-deploy.png`

**This is the most important 2 minutes of your viva.** Put that screenshot up.

> "Anyone can show a green build. I wanted to prove the gate actually works, so I
> deliberately introduced a defect — I removed the flag that highlights students
> below the threshold. The page still renders, the percentages are still correct,
> nothing throws an exception. Every unit test passes.
>
> The Selenium journey caught it. And look at what Jenkins did:
> **`Stage "Deploy" skipped due to earlier failure(s)`.**
>
> Not deployed-and-rolled-back. Never deployed at all. I can prove nothing reached
> the server, because the WAR timestamp on the Tomcat volume was still from the
> previous build. Then I fixed the defect, re-ran, and it deployed normally."

### 5. Honesty (1 min) — this earns marks, it does not lose them

> "Six defects were found by **running** the system rather than reading it, and
> four of them were invisible to a passing test suite. The sharpest one was a
> Selenium test that **passed while testing the wrong user**, because Spring
> Security only accepts logout as a POST and my `GET /logout` silently did nothing.
> A test that passes incorrectly is worse than one that fails."

Then name a real limitation before they ask: H2 instead of PostgreSQL, single node,
no TLS. The full list is in the final report §6.

---

## Likely questions, with short answers

| Question | Answer |
|---|---|
| *Why Selenium if you have unit tests?* | Four of the six real defects were invisible to unit tests. Unit tests assert the code; only driving the running application asserts the product. |
| *Why does a rejected record lower the denominator?* | It is "not yet established", not "absent". An unverified record must not move the official figure either way. |
| *How do you know the gate works?* | Build #7 — Selenium failed and Deploy was skipped. The WAR timestamp proves nothing was deployed. |
| *How is rollback possible?* | Every image is tagged with its build number, not just `latest`. `latest` cannot name the release you want to return to. Rollback took 16 seconds. |
| *How do you prove idempotency?* | The second Ansible run reports `changed=0`. That needed deliberate work on five tasks that would otherwise report `changed` forever. |
| *Why is the workflow in the service layer?* | An illegal transition must be **refused**, not merely un-clickable. A hidden button is not access control. |
| *What would you do differently in production?* | PostgreSQL, TLS, no Docker socket mounted into Jenkins, and blue-green deploys to remove the gap when the container is replaced. |
| *Did you write all this?* | Be straight: you directed the work, made the decisions, and can explain every one of them. Then demonstrate that by answering the next question well. |

---

## If nothing runs on the day

**Do not panic, and do not apologise for ten minutes.** Say this:

> "I'll present from the evidence pack — every step was captured from a real run,
> and each screenshot is committed alongside the raw log it was rendered from."

Then present §3 and §4 entirely from `docs/evidence/`. You have **75 screenshots
and 39 logs**. This is a *stronger* position than most live demos, because a live
demo shows one moment and your pack shows the whole history — including the
failures.

**Never** try to debug live in front of an examiner. Switch to screenshots within
30 seconds and keep talking.

---

## The three things to memorise

1. **"Build #7 — the gate blocked the deployment."**
2. **"A rejected record is 'not yet established', not 'absent'."**
3. **"Six defects found by running it; four were invisible to a green test suite."**

Everything else you can look up in `docs/15-final-report.md` while you talk.

---

## What not to do

- Do not run the Jenkins pipeline live. It takes two minutes of silence and can
  fail for environmental reasons. Use the screenshots.
- Do not open the code and scroll through it unless asked. Examiners ask about
  decisions, not syntax.
- Do not claim it is production-ready. Say what is missing — §6 of the final
  report lists nine limitations, and naming them yourself reads as judgement.
- Do not memorise this document word for word. Memorise the three lines above and
  speak normally.
