---
title: "Consolidate on Vercel + Upstash Redis"
description: "Move bot off Cloudflare Workers + Vercel split-architecture onto a single Vercel deployment. Inline scraper libs (drop store-scraper.vercel.app fetch). Replace CF KV with Upstash Redis. Single repo, single vendor, free tier."
status: pending
priority: P1
effort: 6h
branch: main
tags: [vercel, upstash, migration, scraper-inline, telegram]
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
- **Old `tiennm99/store-scraper` repo:** leave running 1–2 weeks as fallback, then archive

Source design: [`reports/brainstorm-260509-1656-consolidate-vercel-upstash.md`](../reports/brainstorm-260509-1656-consolidate-vercel-upstash.md)
Storage research: [`reports/researcher-260509-1656-upstash-vs-atlas.md`](../reports/researcher-260509-1656-upstash-vs-atlas.md)

## Phases

| # | Phase | Effort | Status |
|---|---|---|---|
| 01 | [Vercel scaffolding + deps](phase-01-vercel-scaffolding.md) | 30 min | pending |
| 02 | [Upstash repository adapter](phase-02-upstash-repository-adapter.md) | 1h | pending |
| 03 | [Inline scraper modules](phase-03-inline-scraper-modules.md) | 45 min | pending |
| 04 | [HTTP layer (webhook + cron)](phase-04-http-layer-webhook-and-cron.md) | 1h | pending |
| 05 | [Data migration CF KV → Upstash](phase-05-data-migration-cf-kv-to-upstash.md) | 30 min | pending |
| 06 | [Deploy + cutover + webhook re-register](phase-06-deploy-cutover-and-webhook-reregister.md) | 45 min | pending |
| 07 | [Cleanup wrangler + docs](phase-07-cleanup-wrangler-and-docs.md) | 30 min | pending |

## Key Constraints

- Free tier only — no $5/mo CF or Vercel Pro upgrades
- Single service — no separate scraper deployment after cutover
- Hard cutover migration; <5 min downtime acceptable
- Telegram parity preserved; all 13 commands keep working
- Daily cron 07:00 Asia/Saigon = 00:00 UTC on Vercel Cron

## Success Criteria (overall)

- Vercel deploy returns 200 on Telegram webhook within 2 s
- Daily cron visible in Vercel Functions log, runs without error
- Admin + group state preserved post-migration (count match)
- `grep -r 'store-scraper.vercel.app' src/` returns zero hits
- `wrangler.toml` and `wrangler` devDep removed from repo
- Bundle size <50 MB; cold start <500 ms

## Dependencies

- Vercel account (already exists — bot will replace existing `store-scraper` project or get a new project)
- Upstash account (new — sign up at upstash.com or via Vercel Marketplace integration)
- Telegram bot already deployed; needs webhook URL re-registration after cutover
- Active CF Worker still running through migration window (rollback safety)
