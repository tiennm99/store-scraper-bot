---
phase: 4
title: Docs + ops reminders
status: completed
priority: P3
effort: 20m
dependencies: []
---

# Phase 4: Docs + ops reminders

## Overview

Add a short "Operations" section to README covering quarterly Upstash credential rotation, links to Vercel + Upstash dashboards (the de-facto observability surface), and a one-liner on the dependabot policy. Also drop the outdated "Preview / unstable" warning at the top — bot is live and the Java cutover is done.

## Architecture

Single-file edit in README. No new docs file — operator notes are short enough to live where deploy + register flow already lives. Future extraction into `docs/operations.md` only if it grows past ~30 lines.

## Related Code Files

**Modify**
- `README.md` — drop "Preview / unstable" blockquote; add `## Operations` section after `## Run`
- `plans/todo.md` — mark items resolved by phases 1–3 done; leave Tests + Observability as remaining

## Implementation Steps

1. Remove the `> ⚠️ Preview / unstable` blockquote (~5 lines near top of README).
2. Add `## Operations` section after `## Run`:
   ```markdown
   ## Operations

   ### Dashboards
   - Vercel project — function logs, cron history, deploy status
   - Upstash console — Redis metrics, key browser, request latency

   ### Credential rotation (quarterly)
   - Upstash REST token — regenerate in Upstash console, update `UPSTASH_REDIS_REST_TOKEN` in Vercel env, redeploy
   - Telegram webhook secret — generate new value, update `TELEGRAM_WEBHOOK_SECRET` in Vercel env, redeploy, then `npm run register`

   ### Dependency security
   - Transitive vulnerabilities from `app-store-scraper → request` are pinned via `overrides` in `package.json`
   - The unfixable `request` SSRF advisory is risk-accepted: only known endpoints (itunes.apple.com, play.google.com) are called; no user-controlled URLs reach `request`
   ```
3. Update `plans/todo.md`: mark phase 1–3 items done; leave Tests + Observability with note that Observability is YAGNI given Vercel/Upstash dashboards.

## Success Criteria

- [ ] README "Preview / unstable" warning removed
- [ ] README has Operations section covering dashboards, rotation, dep security
- [ ] `plans/todo.md` reflects resolved items

## Risk Assessment

- **Calendar reminder:** prose-only docs don't trigger anyone. Mitigation: user adds a calendar event manually — out of scope for code.
- **Doc rot:** rotation steps will go stale if Vercel/Upstash UIs change. Mitigation: keep wording vague ("regenerate in Upstash console") rather than click-by-click.
