---
title: "Deploy js-store-scraper-bot to Cloudflare Workers"
description: "Port Telegram bot from Node.js polling/Mongo-driver/node-cron to Workers webhook + nodejs_compat_v2 + scheduled triggers. MongoDB Atlas M0 free tier retains schema parity with Java/Go ports."
status: pending
priority: P1
effort: 10h
branch: dev
tags: [cloudflare-workers, telegram, mongodb, atlas, deployment]
created: 2026-04-26
blockedBy: []
blocks: []
---

# Plan: Cloudflare Workers Deployment

Port the Node.js polling bot to a Workers `fetch` (Telegram webhook) + `scheduled` (daily check) handler. Free tier only. Storage stays MongoDB (Atlas M0) via `nodejs_compat_v2` — schema parity with `store-scraper-bot` (Java) and `go-store-scraper-bot` preserved.

## Reference Implementation

**[miti99bot](/config/workspace/tiennm99/miti99bot)** — sister project on the same stack (CF Workers + Atlas via `nodejs_compat_v2` + `mongodb` driver). Code-complete, operator-pending. Lift validated patterns; do not re-research.

Key files to crib:
- `wrangler.toml` — `compatibility_flags = ["nodejs_compat_v2"]`, cron triggers, observability block
- `scripts/check-secret-leaks.js` — lint that grep-blocks token leaks
- `docs/using-mongodb.md` — operational runbook

## Constraints (locked)

- **Free plan only.** 100K req/day, 10ms CPU/req baseline (50ms cold start), 3 MiB bundle cap.
- **MongoDB Atlas M0** (free, region `aws-ap-southeast-1`). Schema parity with Java/Go ports.
- **Telegram webhook** (NOT polling). Auth via `secret_token` header (Telegram's first-class mechanism).
- **`nodejs_compat_v2`** (gives `node:net` + `node:tls` for the official `mongodb` driver).
- **Drop these npm deps**: `node-telegram-bot-api`, `node-cron`, `dotenv`, `pino`, `pino-pretty`. Workers ships its own equivalents (`env` bindings, scheduled handler, `console.log`).
- **Keep**: `mongodb` (only addition), business logic in `src/bot/commands/*.js`, models, util.

## Hard Gates

| Gate | Where | Threshold | On fail |
|---|---|---|---|
| **Bundle size** | Phase 01 | `wrangler deploy --dry-run` ≤ 2.7 MiB (10% headroom under 3 MiB Free cap) | **Pivot to Upstash Redis fallback** (out-of-plan) |
| **Cold-start CPU** | Phase 01 | `/__mongo-ping` cold cycles well under 50ms CPU | **Escalate to Workers Paid OR pivot to Upstash** |
| **Auto-pause behavior** | Phase 01 | Paused M0 yields catchable `MongoServerSelectionError` within 5s, not a hang | Document catch path in Phase 03 |

If any gate trips, do NOT continue downstream phases — escalate to user with measured numbers.

## Phases

| # | Phase | Status | Effort | Owner files |
|---|-------|--------|--------|-------------|
| 01 | [Atlas + Wrangler + bundle gate](phase-01-atlas-wrangler-bundle-gate.md) | pending | 2h | `wrangler.toml`, `package.json`, `.dev.vars.example`, `scripts/check-secret-leaks.js`, `docs/using-mongodb.md` |
| 02 | [Worker-native Telegram client](phase-02-telegram-client.md) | pending | 1.5h | `src/bot/telegram-api.js`, `src/bot/bot.js` |
| 03 | [MongoDB Worker adapter](phase-03-mongo-worker-adapter.md) | pending | 1.5h | `src/repository/mongodb.js`, `src/repository/*-repository.js` |
| 04 | [Worker entry — fetch + scheduled](phase-04-worker-entry.md) | pending | 2h | `src/index.js`, `src/bot/dispatch.js`, `src/scheduler/scheduler.js` |
| 05 | [Webhook registration + secrets](phase-05-webhook-registration.md) | pending | 1h | `scripts/register-webhook.js`, `package.json` |
| 06 | [Deploy + smoke + docs](phase-06-deploy-smoke-docs.md) | pending | 2h | `README.md`, `docs/cloudflare-deployment.md` |

## Critical dependencies

- **01 → all** (wrangler config + Atlas creds + bundle gate are prerequisites).
- **02 + 03 → 04** (entry handler wires both into the dispatch + scheduled paths).
- **04 → 05** (need a deployable `fetch` handler before `setWebhook` makes sense).
- **05 → 06** (deploy + smoke is end-to-end; needs webhook live).

## Out of scope (explicit)

- Node-runtime variant retained alongside Workers variant. **No.** Single deployment target. The Node polling code is removed in Phase 04, not preserved with branching.
- Dual-write to KV. Not applicable — no existing CF deployment to migrate from.
- Backfill scripts. Not applicable — no existing data; greenfield collections.
- Tests. The original Node port has none and the user did not request them; revisit if user asks.
- Worker observability tuning beyond `wrangler.toml` defaults from miti99bot.

## Fallback path (if Phase 01 hard gates trip)

**Upstash Redis** (HTTP-native, ~10K req/day free tier):
- Replace `src/repository/*` with thin wrappers over `@upstash/redis`.
- Lose Mongo schema parity with Java/Go ports (acceptable — JS becomes its own deployment).
- Bundle stays tiny (no driver), cold start essentially free.
- Spec lives in `phase-07-alt-upstash-pivot.md` (write only if Phase 01 trips).

## Questions for user (before Phase 01 starts)

1. Atlas account: do you have one, or do you need to create it? (M0 free tier — no credit card required.)
2. Cloudflare account: do you have a Workers free plan attached to a domain? Workers can deploy without a custom domain (`*.workers.dev`); confirm that's acceptable for Telegram webhook URL.
3. Bot username for the webhook: same `TELEGRAM_BOT_USERNAME` as the existing `.env.example`?
4. Any existing data in MongoDB you want imported, or greenfield?

## References

- miti99bot Atlas migration plan: `/config/workspace/tiennm99/miti99bot/plans/260425-1945-mongodb-atlas-migration/`
- CF Workers `nodejs_compat_v2`: https://developers.cloudflare.com/workers/runtime-apis/nodejs/
- Telegram Bot API webhooks: https://core.telegram.org/bots/api#setwebhook (`secret_token` field)
- MongoDB Atlas M0 limits: https://www.mongodb.com/docs/atlas/reference/free-shared-limitations/
