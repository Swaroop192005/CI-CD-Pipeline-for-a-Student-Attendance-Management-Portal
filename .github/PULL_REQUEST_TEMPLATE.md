## What this changes

<!-- One or two sentences. What behaviour is different after this PR? -->

## Related

Closes #<issue>  ·  Backlog ID: `US-__`  ·  Task: `__`

## How it was verified

<!-- Commands actually run, and their result. Not "should work". -->

```
mvn -B clean package
```

- [ ] Unit tests pass locally
- [ ] Selenium suite passes (or: not applicable to this change)
- [ ] Screenshot evidence captured under `docs/evidence/task-NN/`

## Definition of Done

<!-- See docs/02-agile-planning.md section 8 -->

- [ ] Every acceptance criterion on the linked issue is met
- [ ] Business rules covered by unit tests; workflow transitions covered explicitly
- [ ] No hard-coded environment values, no debug printing, no commented-out code
- [ ] `docs/` updated, `docs/00-PROJECT-PLAN.md` status refreshed
- [ ] Jenkins build green on this branch

## Notes for the reviewer

<!-- Anything non-obvious: a trade-off, a deliberate omission, a follow-up issue. -->
