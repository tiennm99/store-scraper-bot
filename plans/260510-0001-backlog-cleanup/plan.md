---
title: 'backlog cleanup: dependabot, CI, bot description'
description: >-
  Knock out the small/concrete items in plans/todo.md. Tests + observability
  remain out of scope.
status: completed
priority: P2
created: 2026-05-10T00:00:00.000Z
---

# backlog cleanup: dependabot, CI, bot description

## Overview

Four small, independent items from `plans/todo.md` bundled into one plan because each is too small to justify its own. Phases are independent — can be merged separately or together.

## Goals & Non-Goals

**Goals**
- Resolve all 4 open Dependabot alerts (1 critical + 3 medium) where a fix exists
- Add minimal GitHub Actions CI (lint + syntax) on PR and push
- Set Telegram bot description / short-description via Bot API
- Document quarterly Upstash credential rotation

**Non-Goals (separate future plans)**
- **Tests** — needs framework choice (vitest vs node:test), conventions, fixtures. Treat as its own multi-phase plan.
- **Observability dashboard** — Vercel + Upstash already provide built-in dashboards. Custom-built dashboard is YAGNI until a real ops question arises that the built-ins can't answer.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Dependabot overrides + audit](./phase-01-dependabot-overrides-audit.md) | Completed |
| 2 | [CI workflow on PR/push](./phase-02-ci-workflow-on-pr-push.md) | Completed |
| 3 | [Bot description script](./phase-03-bot-description-script.md) | Completed |
| 4 | [Docs + ops reminders](./phase-04-docs-ops-reminders.md) | Completed |

## Dependencies

Phases are independent. No cross-plan dependencies.
