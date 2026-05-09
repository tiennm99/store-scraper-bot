# Test Report — Vercel + Upstash Consolidation (Phases 1–5)

**Plan:** [`260509-1656-consolidate-vercel-upstash`](../260509-1656-consolidate-vercel-upstash/plan.md)
**Date:** 2026-05-09
**Note:** Tester subagent hit rate limit before completing. Validation run inline instead. Equivalent depth.

## Validation Results

| # | Check | Result | Evidence |
|---|---|---|---|
| 1 | `npm run lint` | ✓ PASS | `check-secret-leaks: clean` |
| 2 | Syntax check (`node --check`) on all 12 changed/created files | ✓ PASS | 12/12 OK |
| 3 | ESM smoke imports (all repos, scrapers, api/, app-builder) | ✓ PASS | All modules import without runtime error |
| 4 | `vercel.json` parses as valid JSON | ✓ PASS | crons + functions keys present |
| 5 | Stale refs in `src/` `scripts/` `api/` — `STORE_KV`, `cloudflare`, `wrangler`, `BASE_URL`, `store-scraper.vercel.app` | ✓ PASS | Zero hits after fixing scheduler comment |
| 6 | `KEY_PREFIX` wiring — explicit override + default fallback | ✓ PASS | `test-prefix:` honored, `store-scraper-bot:` is default |
| 7 | api/ functions export default + config | ✓ PASS | webhook=`{runtime:nodejs}`, cron=`{runtime:nodejs, maxDuration:60}` |
| 8 | Upstash exports complete | ✓ PASS | `createUpstashClient,getJson,putJson,del,scan,UpstashUnavailable` |

## Skipped

- **Live scraper smoke** (network-dependent, requires real `app-store-scraper` / `google-play-scraper` fetch). Inline lib presence verified at import time; functional smoke deferred to Phase 6 cutover step 8 (manual Telegram smoke).
- **`vercel build --debug`** — requires `vercel login` (operator step). `vercel.json` JSON validity verified instead.
- **No unit tests in repo** — out of scope per plan; documented in `todo.md` backlog.

## Issues Found & Fixed Inline

| Severity | Location | Issue | Fix |
|---|---|---|---|
| Trivial | `src/scheduler/scheduler.js:4-5` | Stale comment said cron lives in `wrangler.toml` | Updated to reference `api/cron.js` + `vercel.json` |

## Status

**Status:** DONE
**Summary:** All Phase 1–5 deliverables compile, import, and lint clean. KEY_PREFIX namespacing works as designed. Zero stale CF references remain.

## Unresolved

- Functional end-to-end smoke (Telegram webhook, cron trigger, real scraper calls) deferred to Phase 6 operator-driven cutover.
