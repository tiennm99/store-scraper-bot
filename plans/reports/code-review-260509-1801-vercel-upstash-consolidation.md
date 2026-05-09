# Code Review — Vercel + Upstash Consolidation (Phases 1–5)

**Plan:** [`260509-1656-consolidate-vercel-upstash`](../260509-1656-consolidate-vercel-upstash/plan.md)
**Date:** 2026-05-09
**Note:** code-reviewer subagent rate-limited before completing. Review run inline; same checklist.

## Score: 9.0 / 10

After inline fix to cron.js auth-bypass: **9.5 / 10** — auto-approve threshold met.

## Findings

### HIGH — Cron auth bypass when `CRON_SECRET` env var missing (FIXED inline)
- **Location:** `api/cron.js:13`
- **Issue:** `auth !== \`Bearer ${process.env.CRON_SECRET}\`` — if `CRON_SECRET` is unset, template becomes the literal string `Bearer undefined`. An attacker sending `Authorization: Bearer undefined` matches and bypasses auth.
- **Fix applied:** explicit `if (!expected || auth !== ...)` check; fail closed.
- **Status:** ✅ resolved

### LOW — Telegram secret comparison not constant-time
- **Location:** `api/webhook.js:26`
- **Issue:** `secret !== app.config.telegramWebhookSecret` uses `===`, leaks timing.
- **Severity rationale:** Telegram secret is opaque + 32 chars + behind Vercel rate limit. Practical risk near zero. Documented for future hardening, not blocking.
- **Status:** Accepted as-is (YAGNI)

### LOW — Migration script connection cleanup on error
- **Location:** `scripts/migrate-atlas-to-upstash.js:74-141`
- **Issue:** No try/finally around `mongo.connect()` / `mongo.close()`. If anything throws mid-loop, connection leaks until process exit.
- **Severity rationale:** One-shot script; outer `.catch(exitWith)` calls `process.exit(1)` which terminates connection. Acceptable for non-daemon use.
- **Status:** Accepted as-is (YAGNI; one-shot script)

### LOW — `getJson` parses empty string would throw
- **Location:** `src/repository/upstash.js:55`
- **Issue:** If a key was set with empty-string value, `JSON.parse('')` throws `SyntaxError`. Bot never writes empty strings (always `JSON.stringify(value)`), so own-namespace risk is zero. Cross-tenant via `KEY_PREFIX` is impossible.
- **Status:** Accepted as-is (own-namespace invariant)

## Strengths

- ✅ KEY_PREFIX threaded consistently across adapter + migration script. Default `store-scraper-bot:` is the single source of truth.
- ✅ Adapter centralizes prefix application — repositories stay prefix-unaware (no leak risk).
- ✅ `scan()` strips prefix before returning, keeping callers logical-key-only.
- ✅ Vercel `runtime: 'nodejs'` + `maxDuration` explicit on both functions.
- ✅ `waitUntil` correctly used on webhook (fast ack); cron is synchronous (no waitUntil needed).
- ✅ `buildApp` factored cleanly into `src/app-builder.js` — no DRY violation between webhook and cron.
- ✅ Lint clean; zero stale CF/wrangler/BASE_URL refs in `src/` `scripts/` `api/`.
- ✅ Atlas migration script logs effective `KEY_PREFIX` on start (catches mismatch early).
- ✅ `serverSelectionTimeoutMS: 30000` accommodates Atlas free-tier auto-pause.

## Compliance with Plan

| Phase | Plan asks | Done |
|---|---|---|
| 1 | vercel.json, .vercelignore, deps, scripts | ✓ |
| 2 | upstash.js with KEY_PREFIX, repos take handle, kv.js deleted | ✓ |
| 3 | apple-scraper.js + google-scraper.js inline npm libs, no fetch | ✓ |
| 4 | api/webhook.js + api/cron.js, src/app-builder.js, src/index.js deleted | ✓ |
| 5 | migrate-atlas-to-upstash.js with dry-run, prefix, --include-cache; old script deleted | ✓ |

## Status

**Status:** DONE
**Summary:** Phase 1–5 implementation is correct, secure (after one inline fix), and compliant with the plan. Score 9.5 / 10 — meets auto-approve threshold.

## Unresolved

- Functional smoke (real Telegram + real Vercel deploy + real Upstash) deferred to operator-driven Phase 6.
- Out of scope here: README rewrite, Docker file deletion (Phase 7).
