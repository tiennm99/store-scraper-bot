# Outstanding Work

Quick index of active direction. CF Workers path superseded — bot runs on Vercel + Upstash.

## Completed plans

**[260509-1656-consolidate-vercel-upstash](260509-1656-consolidate-vercel-upstash/plan.md)** — Vercel deploy, Atlas → Upstash data migration, Docker + wrangler cleanup. 7 phases done.

## Superseded plans (left for history; do not execute)

- [260426-2327-cloudflare-deploy-and-smoke](260426-2327-cloudflare-deploy-and-smoke/plan.md) — CF Workers deploy. Never completed; superseded by Vercel direction.
- [260505-1425-cloudflare-kv-migration-and-deploy](260505-1425-cloudflare-kv-migration-and-deploy/plan.md) — CF KV migration. Superseded.

## Pre-flight (operator)

Before running the active plan:

- [ ] Vercel account ready (link or create new project)
- [ ] Upstash account ready (free Redis, signup at upstash.com or via Vercel Marketplace)
- [ ] Read access to legacy Java bot's MongoDB Atlas (one-shot for Phase 5 migration)
- [ ] Java bot can be paused for ~5 min during Phase 6 cutover

## Backlog (post-deploy, out of current scope)

- [ ] Tests (none exist; original Node port has none either)
- [ ] Quarterly Atlas password rotation reminder
- [ ] Observability dashboard (CF Workers + Atlas charts)
- [ ] CI workflow on push (lint + dry-run bundle size as PR check)
- [ ] Telegram bot description / about-text via `setMyDescription` (post-deploy)

## Reference

- **miti99bot** validated the same stack (CF Workers + Atlas + `nodejs_compat_v2` + `mongodb`): `/config/workspace/tiennm99/miti99bot/plans/260425-1945-mongodb-atlas-migration/`
- **Archived code-port plan** (history of what's now done): [archive/260426-2015-cloudflare-worker-code-port](archive/260426-2015-cloudflare-worker-code-port/plan.md)
