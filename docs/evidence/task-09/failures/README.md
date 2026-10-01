# Failure capture — demonstration

These artefacts are **real output from a real failing run**, produced by
deliberately removing the SHORTAGE flag from `dashboard.html` and running the
suite. The defect was reverted immediately afterwards.

| File | Contents |
|---|---|
| `EXAMPLE-j5-shortage-flag-missing.png` | The dashboard at the moment of failure |
| `EXAMPLE-j5-shortage-flag-missing.txt` | Test name, URL and the assertion error |

The screenshot is diagnostic rather than decorative: 22CS003 is visibly at **0%**
and the row is tinted, yet the **SHORTAGE badge is gone** — which is precisely the
injected defect. A reviewer can identify the bug from the image without reading a
stack trace.

On a failing run the extension also writes a `.html` page source next to these,
which answers what a picture cannot (for example, whether an element was present
but not visible). It is excluded here only to keep the repository small.

**Screenshots are written for failures and only for failures.** A folder of
passing screenshots is noise that hides the one image that matters.
