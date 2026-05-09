---
title: "Consolidate on Vercel + Upstash Redis"
description: "Deploy bot to Vercel (no Cloudflare). Migrate live state from legacy java-store-scraper-bot MongoDB Atlas → Upstash Redis. Inline scraper libs (drop store-scraper.vercel.app fetch). Delete Docker artifacts. Single repo, single vendor, free tier."
status: completed
priority: P1
effort: 6h
branch: main
tags: [vercel, upstash, migration, scraper-inline, telegram, atlas-migration, docker-cleanup]
created: 2026-05-09
blockedBy: []
blocks: []
supersedes: 260505-1425-cloudflare-kv-migration-and-deploy
---

# Consolidate on Vercel + Upstash

Replaces the in-progress Cloudflare Workers + KV direction. Brainstorm + research locked these decisions:

- **Compute:** Vercel serverless functions (Hobby plan, free)
- **Storage:** Upstash Redis (free 500k cmd/mo, persistent RocksDB)
- **Scraper:** inline `app-store-scraper` + `google-play-scraper` npm libs, drop `store-scraper.vercel.app` HTTP roundtrip
- **Data source:** legacy `java-store-scraper-bot` MongoDB Atlas (still serving production); Node port never deployed live, so CF KV is **not** a migration source
- **Cleanup:** delete Dockerfile + docker-compose*.yml (Mongo-based local dev path obsolete with Upstash REST); delete wrangler.toml; delete legacy migration script after one-shot run
- **Old `tiennm99/store-scraper` repo:** leave running 1–2 weeks as fallback, then archive

Source design: [`reports/brainstorm-260509-1656-consolidate-vercel-upstash.md`](../reports/brainstorm-260509-1656-consolidate-vercel-upstash.md)
Storage research: [`reports/researcher-260509-1656-upstash-vs-atlas.md`](../reports/researcher-260509-1656-upstash-vs-atlas.md)

## Phases

| # | Phase | Effort | Status |
|---|---|---|---|
| 01 | [Vercel scaffolding + deps](phase-01-vercel-scaffolding.md) | 30 min | completed |
| 02 | [Upstash repository adapter](phase-02-upstash-repository-adapter.md) | 1h | completed |
| 03 | [Inline scraper modules](phase-03-inline-scraper-modules.md) | 45 min | completed |
| 04 | [HTTP layer (webhook + cron)](phase-04-http-layer-webhook-and-cron.md) | 1h | completed |
| 05 | [Data migration MongoDB Atlas → Upstash](phase-05-data-migration-atlas-to-upstash.md) | 30 min | completed |
| 06 | [Deploy + cutover + webhook register](phase-06-deploy-cutover-and-webhook-reregister.md) | 45 min | completed |
| 07 | [Cleanup wrangler + docker + docs](phase-07-cleanup-wrangler-and-docs.md) | 30 min | completed |

## Key Constraints

- Free tier only — no $5/mo CF or Vercel Pro upgrades
- Single service — no separate scraper deployment after cutover
- Hard cutover migration; <5 min downtime acceptable
- Telegram parity preserved; all 13 commands keep working
- Daily cron 07:00 Asia/Saigon = 00:00 UTC on Vercel Cron
- Java bot stays warm 7 days post-cutover for rollback only (then retire)
- **Multi-tenant safe:** all Upstash keys carry `KEY_PREFIX` (default `store-scraper-bot:`) so the same Upstash DB can be shared with other Vercel projects without collision. Prefix is enforced at the adapter layer; repositories stay prefix-unaware.

## Success Criteria (overall)

- Vercel deploy returns 200 on Telegram webhook within 2 s
- Daily cron visible in Vercel Functions log, runs without error
- Admin + group state preserved across Atlas → Upstash (count match)
- All Upstash keys carry `KEY_PREFIX`; zero keys leaked into the shared namespace
- `grep -r 'store-scraper.vercel.app' src/` returns zero hits
- `wrangler.toml` + `wrangler` devDep removed; `Dockerfile` + `docker-compose*.yml` removed
- `mongodb` devDep removed (only needed for one-shot Phase 5 migration)
- Bundle size <50 MB; cold start <500 ms

## Dependencies

- Vercel account (already exists — bot will replace existing `store-scraper` project or get a new project)
- Upstash account (new — sign up at upstash.com or via Vercel Marketplace integration)
- MongoDB Atlas read access on legacy Java bot's cluster (one-shot, Phase 5)
- Java bot keeps running until cutover (Phase 6); idle but available 7 days post for rollback
