---
title: "Cloudflare KV migration + deploy"
description: "Replace MongoDB Atlas with Cloudflare KV as the storage layer, deploy to Workers, register Telegram webhook, smoke-test, and document. Supersedes the Atlas-based deploy plan."
status: pending
priority: P1
effort: 5h
branch: main
tags: [cloudflare-workers, cloudflare-kv, deploy, migration]
created: 2026-05-05
blockedBy: []
blocks: [260426-2327-cloudflare-deploy-and-smoke]
---

# Cloudflare Workers + KV Storage Migration

User chose KV over Atlas to pre-empt the bundle-size and cold-start risks called out in the Atlas plan. Drops the `mongodb` driver entirely (~4–5 MiB compressed) and the `nodejs_compat_v2` flag along with it.

## Why KV fits this codebase

Every existing repository operation is a primary-key lookup — no queries, no aggregations, no cursors:

| Logical collection | Operations | KV key shape |
|---|---|---|
| `common` (admin singleton) | get/save the single admin doc with `groups[]` | `admin` |
| `group` (per-chat) | exists/get/save/delete by groupId | `group:{groupId}` |
| `apple_app` (cache) | get/save by appId, TTL expiry | `apple:{appId}` (with `expirationTtl`) |
| `google_app` (cache) | get/save by appId, TTL expiry | `google:{appId}` (with `expirationTtl`) |

KV's native `expirationTtl` replaces the manual `isExpired(entry, now, cacheMs)` check.

The daily cron reads `admin.groups[]` (single key), then iterates groups (one read each). No list-keys needed.

## Phases

| # | Phase | Status | Effort |
|---|---|---|---|
| 01 | [Replace Mongo with KV](phase-01-kv-repositories.md) | completed | 1.5h |
| 02 | [Wrangler + KV namespace](phase-02-wrangler-and-kv-namespace.md) | pending | 0.5h |
| 03 | [Deploy + bundle gate](phase-03-deploy-and-bundle-gate.md) | pending | 0.5h |
| 04 | [Register webhook + smoke](phase-04-register-webhook-and-smoke.md) | pending | 1.5h |
| 05 | [Docs + cleanup](phase-05-docs-and-cleanup.md) | pending | 1h |
| 06 | [Atlas → KV data migration](phase-06-atlas-data-migration.md) | pending | 1h |

## Critical dependencies

- 01 → 02 (wrangler binding name must match what code reads from `env.STORE_KV`)
- 02 → 03 (deploy needs the namespace ID provisioned)
- 02 → 06 (data migration writes via `wrangler kv bulk put`, needs namespace)
- 03 → 04 (webhook registration needs live Worker URL)
- 06 → 04 (smoke tests need migrated data — except clean greenfield runs)

## Hard gates (now sanity checks, not pivots)

| Gate | Threshold | Notes |
|---|---|---|
| Bundle size | ≤ 500 KiB (well under 3 MiB Free cap) | Without `mongodb`, bundle should drop to ~30 KiB |
| Cold start | < 10ms CPU | No TCP handshake; KV access is HTTP-fetched edge-side |

If bundle exceeds 500 KiB, investigate — something pulled in unintended deps.

## Key design decisions

1. **One KV namespace, prefixed keys** — cleaner than four namespaces; KV billing is by op count not namespace.
2. **Drop `nodejs_compat_v2`** — only needed for the Mongo TCP socket. Removing reduces cold-start and runtime memory.
3. **Drop `mongodb` dependency** — removes ~4–5 MiB compressed. `npm uninstall mongodb`.
4. **Cache TTL via `expirationTtl`** — let KV handle expiry; remove `isAppleAppExpired`/`isGoogleAppExpired` helpers from the read path.
5. **Document model unchanged** — same `_id`, `class`, fields. Stored as JSON values. Java/Go parity preserved at the document level even though the storage engine differs.

## Java parity verification

Cross-checked against existing parity reports (commit `e3e375d`):
- `plans/reports/xia-260429-1601-store-scraper-bot-port-status.md` — 11 PARITY · 2 MINOR · **0 GAP** · 3 EXTRA
- `plans/reports/researcher-260429-1555-java-vs-js-parity.md` — schema, scheduler timing, cache TTL, HTML formatting all confirmed identical

**KV swap preserves every PARITY item.** Document shape (`_id`, `class`, fields) is written verbatim as JSON values. Cache TTL via `expirationTtl` is behaviorally equivalent to Java's `(now - millis) > cacheMillis` read-side check (same cache-miss→refetch boundary). `countDocuments` and `findOne` map cleanly to `get !== null` and `get`.

Two intentional divergences from Java/Mongo, both accepted:
1. KV ~60s eventual consistency (Java/Mongo was strongly consistent) — fine for single-admin daily-cron use.
2. KV `expirationTtl` 60s floor (Java had no minimum) — mitigated by a clamp in `kv.js`.

## Known KV trade-offs (accepted)

- **Eventual consistency (~60s global propagation)**: `/addgroup` followed by an immediate cron run on a different POP could miss the new group. For this single-admin bot with a once-daily cron, acceptable.
- **No atomic read-modify-write**: Two concurrent `/addgroup` calls could clobber each other on the singleton's `groups[]` array. Mongo's `replaceOne` had the same race; not a regression.
- **Free tier**: 100k reads/day, 1k writes/day, 1 GB storage. Bot usage projects <100 reads/day.

## Out of scope

- ~~Data migration — greenfield per `plans/todo.md` Q3.~~ Now in scope as Phase 06.
- Tests — none exist; not adding here.
- Custom domain — `*.workers.dev` URL is fine for Telegram webhooks.
- Durable Objects / D1 — KV is sufficient.

## Rollback path

- After Phase 01 (code-only): `git revert` reverts the repo refactor; `mongodb` driver back, plan reopens against Atlas-based deploy plan.
- After Phase 03 (deployed but no webhook): `wrangler delete` removes Worker.
- After Phase 04 (webhook live): `setWebhook` with empty URL clears it; rotate `TELEGRAM_WEBHOOK_SECRET` so any in-flight POSTs 401.
- KV namespace can be retained — empty namespaces cost nothing on Free tier.

## Supersedes

This plan replaces the storage backend chosen in [`260426-2327-cloudflare-deploy-and-smoke`](../260426-2327-cloudflare-deploy-and-smoke/plan.md). On completion, that plan should be archived.
