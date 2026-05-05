---
title: "Cloudflare Workers deploy + smoke + docs"
description: "Operator-driven phases: provision Atlas, set CF secrets, run hard gates, deploy, register webhook, smoke-test all 13 commands, write docs. Sister to the code-port plan."
status: pending
priority: P1
effort: 4h
branch: main
tags: [cloudflare-workers, deploy, smoke, atlas]
created: 2026-04-26
blockedBy: [260426-2015-cloudflare-worker-code-port, 260505-1425-cloudflare-kv-migration-and-deploy]
blocks: []
supersededBy: 260505-1425-cloudflare-kv-migration-and-deploy
---

> **Superseded** — User chose Cloudflare KV over Atlas pre-emptively, before the hard-gate trip. Deploy work is now tracked in [`260505-1425-cloudflare-kv-migration-and-deploy`](../260505-1425-cloudflare-kv-migration-and-deploy/plan.md). This file is kept for history.

# Plan B — Deploy + Smoke

Run only after [the code-port plan](../260426-2015-cloudflare-worker-code-port/plan.md) is complete and committed. This plan touches **real accounts**: Atlas (free M0) and Cloudflare (free Workers).

## Reference Implementation

[miti99bot Phase 01 atlas-setup](/config/workspace/tiennm99/miti99bot/plans/260425-1945-mongodb-atlas-migration/phase-01-atlas-setup.md) — proven Atlas + nodejs_compat_v2 procedure. Lift the operator steps wholesale.

## Hard Gates (can abort the plan → pivot to Upstash Redis)

| Gate | Phase | Threshold | On fail |
|---|---|---|---|
| Bundle size | 02 | `wrangler deploy --dry-run` ≤ 2.7 MiB (10% headroom under 3 MiB Free cap) | Pivot to Upstash (out-of-scope here; plan + execute separately) |
| Cold-start CPU | 02 | `/__mongo-ping` cold cycles < 40ms CPU | Escalate to Workers Paid ($5/mo) OR pivot to Upstash |
| Auto-pause behavior | 02 | Paused M0 yields catchable error within 5s | Document catch path requirement; not abort-worthy |

## Phases

| # | Phase | Status | Effort |
|---|---|---|---|
| 01 | [Atlas M0 + CF secrets](phase-01-atlas-and-secrets.md) | pending | 1h |
| 02 | [First deploy + bundle/CPU gates](phase-02-deploy-and-gates.md) | pending | 1h |
| 03 | [Register webhook](phase-03-register-webhook.md) | pending | 0.5h |
| 04 | [End-to-end smoke + docs](phase-04-smoke-and-docs.md) | pending | 1.5h |

## Critical dependencies
- 01 → 02 (deploy needs MONGODB_URI + secrets)
- 02 → 03 (register needs live worker URL + passing gates)
- 03 → 04 (smoke needs registered webhook)

## Prerequisites (operator must have)
- MongoDB Atlas account (free, no credit card).
- Cloudflare account (free, no credit card).
- `wrangler login` completed locally.
- Telegram bot token (existing — same as Node deployment).

## Out of scope (explicit)
- Application code changes — handled by code plan; this plan deploys what's already on `main`.
- Custom domain — `*.workers.dev` URL is fine for Telegram webhooks.
- Tests — original project has none; not adding here.
- Upstash pivot — only triggered if hard gates trip; planned then, not now.

## Rollback path
- After Phase 02 (deploy live but not registered): `wrangler delete` removes Worker; no public surface.
- After Phase 03 (webhook registered): `setWebhook` with empty URL clears it; or rotate `TELEGRAM_WEBHOOK_SECRET` so future POSTs 401.
- After Phase 04 (production live): `git revert` to pre-Workers commit, redeploy via Docker compose for the existing Node polling path.

## Questions to confirm before Phase 01
1. Atlas account: do you have one, or first-time setup?
2. CF account: free Workers plan attached?
3. Bot username for `setMyCommands`: same as in `.env.example`?
4. Greenfield Mongo data, or existing data to import? (If existing, schedule a separate import phase.)
