---
title: per-group days-to-warning setting + settings commands
description: >-
  Allow each Telegram group to override numDaysWarningNotUpdated. Add /settings
  (read) and /setdayswarning (write).
status: completed
priority: P2
created: 2026-05-09T00:00:00.000Z
---

# per-group days-to-warning setting + settings commands

## Overview

Today `numDaysWarningNotUpdated` is global env config (default 30). One threshold for all groups. Goal: let each group set its own threshold, falling back to env default. Also expose generic `/settings` reader so future per-group settings (timezone, silent days, etc.) drop in without new commands.

## Goals & Non-Goals

**Goals**
- `group.settings.numDaysWarningNotUpdated` optional override, persisted in Upstash with the rest of group state
- `/settings` — show all settings for the calling group (with effective + default values)
- `/setdayswarning <n>` — set the threshold (1..3650 integer); `0` or empty resets to default
- Scheduler + `/checkapp` use the per-group resolved threshold

**Non-Goals**
- Other settings (timezone, silent weekend, language) — schema is extensible but no values added yet
- Admin-only restriction — `authorizeGroup` only (matches sibling commands like `/addapple`)
- Migration of existing groups — schema is additive; missing `settings` reads as `{}`

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Schema + threshold resolver](./phase-01-schema-threshold-resolver.md) | Completed |
| 2 | [Commands + wiring](./phase-02-commands-wiring.md) | Completed |
| 3 | [Smoke test](./phase-03-smoke-test.md) | Completed |

## Dependencies

None. Self-contained change. Depends only on existing Upstash group repository + command pattern.
