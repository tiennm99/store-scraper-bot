---
phase: 3
title: Smoke test
status: completed
priority: P3
effort: 15m
dependencies:
  - 2
---

# Phase 3: Smoke test

## Overview

No unit-test suite exists. Validate via syntax checks, the existing secret-leak lint, and a manual Telegram run-through against the deployed bot.

## Implementation Steps

1. `node --check` on every modified/new file:
   - `src/util/group-settings.js`
   - `src/repository/group-repository.js`
   - `src/scheduler/scheduler.js`
   - `src/bot/commands/check-app.js`
   - `src/bot/commands/get-settings.js`
   - `src/bot/commands/set-days-warning.js`
   - `src/bot/bot.js`
2. `npm run lint` (secret-leak scan, must pass).
3. Deploy preview to Vercel (or merge to `main` if direct-deploy is normal).
4. Manual Telegram smoke (in an authorized test group):
   - `/settings` → table shows `numDaysWarningNotUpdated = (unset) / default 30` (or whatever env value is)
   - `/setdayswarning 5` → "Days-to-warning set to 5"
   - `/settings` → shows `5 / 30`
   - `/checkapp` → header reads `>5 days`, results reflect 5-day threshold
   - `/setdayswarning 0` → "Reset to default (30d)"
   - `/settings` → back to unset / 30
   - `/setdayswarning abc` → "Invalid arguments"
   - `/setdayswarning 9999` → "Invalid arguments"
   - From an unauthorized chat: `/settings` → "Group is not allowed to use bot"
5. Wait for next daily cron OR force-trigger via Vercel cron-now panel; confirm authoritized group with override sees correct threshold in report header.

## Success Criteria

- [ ] All `node --check` pass
- [ ] `npm run lint` passes
- [ ] Manual Telegram run-through hits every bullet above
- [ ] Daily cron report header reflects per-group override (or shows env default for groups with no override)

## Risk Assessment

- **No automated regression:** future refactors could silently break the resolver. Acceptable for now — repo has no tests at all. Document the resolver invariants in the file's leading comment.
- **Cron timing:** waiting up to 24h for natural cron firing slows verification. Mitigation: hit `/api/cron` directly with `Authorization: Bearer $CRON_SECRET` to force a run.
