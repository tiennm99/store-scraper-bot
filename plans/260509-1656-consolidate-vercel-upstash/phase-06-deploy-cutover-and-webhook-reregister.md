---
phase: 6
title: "Deploy + cutover + webhook re-register"
status: completed
priority: P1
effort: 45 min
dependencies: [4, 5]
---

# Phase 6: Deploy + cutover + webhook re-register

## Overview

Deploy Vercel project, configure secrets, run migration, flip Telegram webhook URL. Hard cutover with <5 min downtime.

## Requirements

- Vercel project deployed with all secrets set
- Upstash database provisioned (free tier)
- Telegram webhook points to new Vercel URL
- All 13 commands smoke-test pass on production
- Java bot (legacy) kept running but webhook detached — rollback path for ~7 days

## Architecture

Sequence (operator-driven, ~30 min wall clock):

```
1. PROVISION
   Upstash → create free Redis DB → copy REST_URL + REST_TOKEN
   Vercel → create project from repo or link existing

2. SECRETS (Vercel env vars)
   TELEGRAM_BOT_TOKEN
   TELEGRAM_BOT_USERNAME
   TELEGRAM_WEBHOOK_SECRET     (≥32 chars random)
   ADMIN_IDS                    (comma-separated)
   UPSTASH_REDIS_REST_URL
   UPSTASH_REDIS_REST_TOKEN
   CRON_SECRET                  (≥32 chars random; for cron auth)
   KEY_PREFIX=store-scraper-bot:   (namespace this bot's keys; must match what migration script used)
   APP_CACHE_SECONDS=600
   NUM_DAYS_WARNING_NOT_UPDATED=30

3. DEPLOY
   vercel deploy --prod         (gets URL like store-scraper-bot-xxx.vercel.app)

4. SMOKE (no traffic yet)
   curl -X POST $URL/api/webhook → 401 (no secret) → confirms route alive
   curl with valid secret + dummy body → 200

5. MIGRATE DATA (downtime starts)
   Stop Java bot polling: docker compose down (or systemctl stop) on the Java host
   → no further writes land on Atlas
   npm run migrate              (Atlas → Upstash, reads MONGODB_URI from .env.deploy)
   verify admin + group counts in Upstash dashboard match Atlas

6. FLIP
   .env.deploy: WORKER_URL=https://<vercel-url>/api/webhook   (var name kept; existing register-webhook.js reads it)
   npm run register             (registers webhook for the first time + secret)
   wait 30 s for Telegram propagation

7. SMOKE COMMANDS (production)
   /info → returns chat ID
   /listgroup (admin) → lists migrated groups
   /listapp (in known group) → lists tracked apps
   /checkapp → fetches via inlined scrapers
   ... (run all 13 commands per docs/code-standards.md or tester checklist)

8. WATCH
   tail Vercel function logs 30 min for error spikes
   verify daily cron at 00:00 UTC fires (next morning)
```

## Related Code Files

- Modify: `.env.deploy` (operator file, gitignored — change `WORKER_URL`)
- No code changes; this phase is operator-driven

## Implementation Steps

1. Sign up Upstash → create Redis database (region matching Vercel deploy region — `us-east-1` default).
2. Vercel: link existing project OR create new from `tiennm99/store-scraper-bot` repo. Set all env vars listed above via `vercel env add` or dashboard.
3. `vercel deploy --prod`. Note returned URL.
4. Update `.env.deploy`: set `WORKER_URL` to `https://<vercel-url>/api/webhook` (variable name retained for existing script); ensure `MONGODB_URI` (Atlas, read-only OK) and `UPSTASH_REDIS_REST_URL`/`_TOKEN` are present.
5. Stop the Java bot (long-poll consumer) so no further writes hit Atlas. Confirm in Telegram by sending a no-op command and seeing it not get answered.
6. Run `npm run migrate` (Phase 5 script: Atlas → Upstash). Verify counts in Upstash dashboard match Atlas.
7. Run `npm run register` to set webhook to Vercel URL.
8. Smoke all 13 commands manually in Telegram. Use `docs/` test checklist if present.
9. Tail Vercel logs: `vercel logs <project> --follow` for 15 min.
10. Document Vercel URL + Upstash DB name in plan/journal.

## Success Criteria

- [ ] `getWebhookInfo` shows new Vercel URL
- [ ] All 13 commands smoke-pass on production (manually checked)
- [ ] Daily cron fires at 00:00 UTC (verify in Vercel Functions log next morning)
- [ ] Vercel function logs show no error-level entries for 30 min
- [ ] Upstash dashboard shows `<prefix>admin` + `<prefix>group:*` keys with expected count
- [ ] No keys exist in Upstash without the configured `KEY_PREFIX` (confirms isolation; nothing leaked into shared namespace)

## Risk Assessment

- **Risk:** Vercel function cold start exceeds Telegram 30 s ack window on first hit. **Mitigation:** ack returns 200 OK fast (before `waitUntil` heavy work); cold start typically <500 ms.
- **Risk:** Migration loses TTL on cache entries. **Mitigation:** acceptable (cache rebuilds in 10 min). Skipped by default.
- **Risk:** Smoke missed an edge case → user-visible regression. **Mitigation:** Java bot kept idle but available for 7 days; rollback = restart Java bot polling + clear Telegram webhook. ~5 min recovery (Java bot picks up via long polling once webhook is cleared). Note: any commands handled by Vercel during the rollback window won't be in Atlas — accept minor state divergence on rollback.
- **Risk:** Vercel free tier rate-limits cold starts during smoke. **Mitigation:** 100 invocations/day quota is plenty; smoke uses ~20.
- **Risk:** Operator forgets `CRON_SECRET` → cron handler always 401. **Mitigation:** verify by triggering cron manually via Vercel dashboard "Run Now" before relying on schedule.
