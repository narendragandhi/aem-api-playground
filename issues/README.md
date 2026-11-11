# Tracked gaps and fixed issues

This directory tracks known fidelity gaps (behaviors that differ from the
intended AEM API surface or are unsupported) and issues that have since been
fixed, in the style of [aws/aws-emulator](https://github.com/aws/aws-emulator).

Two folders:

- `things-that-do-not-work/` — APIs, behaviors, or surfaces that don't work,
  work partially, or carry caveats. One file per issue.
- `things-that-now-work/` — issues that have been fixed. Move the file here
  when fixed and update the status line.

## File format

Keep each file short and factual:

```markdown
# <short title>

**Status:** does not work / partially works / fixed (<YYYY-MM-DD>)
**Affects:** `<command>` ... — AEM versions affected
**Why:** root cause or missing endpoint
**Symptoms:** what the user sees
**Workaround:** how to work around it, if one exists
```

The README "Caveats" and "Current limitations" sections summarize the current
gaps; this directory is the detailed, per-issue source of truth. Update both:
when a gap is fixed, move the file to `things-that-now-work/` and adjust the
README wording.
