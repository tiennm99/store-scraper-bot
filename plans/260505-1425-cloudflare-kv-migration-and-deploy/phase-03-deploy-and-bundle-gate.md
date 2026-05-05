---
phase: 3
title: "Deploy + bundle-size sanity gate"
status: pending
priority: P1
effort: "0.5h"
dependencies: [2]
---

# Phase 03: Deploy + bundle-size sanity gate

## Overview

Set Worker secrets, run `wrangler deploy`, confirm the bundle is well under the Free 3 MiB cap, and verify a cold-start request reaches the Worker.

## Requirements

**Functional**
- Worker is reachable at `https://js-store-scraper-bot.<account>.workers.dev`.
- A request without the secret header returns 401.

**Non-functional**
- Bundle ≤ 500 KiB (informational; we expect ~30 KiB without `mongodb`).
- Cold start CPU < 10ms.

## Architecture

`wrangler deploy` reads secrets from the Cloudflare account (set via `wrangler secret put`), KV bindings from `wrangler.toml`, and uploads the bundled Worker. No Atlas, no TCP — just KV (HTTP edge-side), `fetch` to Telegram + iTunes + Google Play.

Required secrets (set once via `wrangler secret put <NAME>`):

| Secret | Purpose |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Posting to Telegram Bot API |
| `TELEGRAM_BOT_USERNAME` | Bot mention parsing |
| `TELEGRAM_WEBHOOK_SECRET` | Validates `X-Telegram-Bot-Api-Secret-Token` |
| `ADMIN_IDS` | Comma-separated Telegram user IDs |

`MONGODB_URI` is **no longer required**. If a previous deploy set it, leave it (no-op) or run `wrangler secret delete MONGODB_URI` for hygiene.

## Related Code Files

**No code changes.** Phase deals with deployment only.

## Implementation Steps

1. **Set secrets** (interactive prompts; values supplied by operator):
   ```sh
   npx wrangler secret put TELEGRAM_BOT_TOKEN
   npx wrangler secret put TELEGRAM_BOT_USERNAME
   npx wrangler secret put TELEGRAM_WEBHOOK_SECRET
   npx wrangler secret put ADMIN_IDS
   ```
   `TELEGRAM_WEBHOOK_SECRET`: any random ≥32 chars, e.g. `openssl rand -hex 24`.

2. **Optional cleanup**: `npx wrangler secret delete MONGODB_URI` if it was set previously.

3. **Bundle gate**:
   ```sh
   npx wrangler deploy --dry-run --outdir=.bundle 2>&1 | tee .bundle/dry-run.log
   du -h .bundle/*.js
   ```
   Expected: total .js output < 500 KiB. If > 500 KiB, abort and investigate (`wrangler deploy --dry-run --metafile` then inspect imports).

4. **Real deploy**:
   ```sh
   npx wrangler deploy
   ```
   Capture the deployed URL.

5. **Cold-start probe**: send a probe with a bad secret to confirm the 401 path works (cheap, doesn't touch KV):
   ```sh
   curl -i -X POST https://js-store-scraper-bot.<account>.workers.dev \
        -H "X-Telegram-Bot-Api-Secret-Token: wrong" \
        -d '{}'
   ```
   Expected: `HTTP/1.1 401 Unauthorized`.

6. **CPU check**: open the Cloudflare dashboard → Workers → js-store-scraper-bot → Logs/Metrics. After a few requests, confirm cold-start CPU is well under 50ms (target <10ms without Mongo).

## Todo List

- [ ] Set 4 required secrets via `wrangler secret put`
- [ ] (Optional) Delete obsolete `MONGODB_URI` secret
- [ ] Run `wrangler deploy --dry-run`; verify bundle < 500 KiB
- [ ] `wrangler deploy`; capture URL
- [ ] curl probe → expect 401
- [ ] Confirm cold-start CPU < 10ms in CF dashboard

## Success Criteria

- [ ] Bundle size logged and < 500 KiB.
- [ ] `wrangler deploy` succeeds with `Uploaded` line.
- [ ] 401 probe returns 401 status.
- [ ] CPU per request < 10ms in dashboard logs.

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Bundle balloons due to a transitive dep | Very Low | `mongodb` was the only heavy dep; with it gone, only stdlib + small `wrangler` runtime hooks remain |
| CF account quota exceeded | Very Low | New account = 100k requests/day budget; bot traffic is single-digit per minute |
| Secret typo (e.g. token) → 401 from Telegram on first webhook hit | Medium | Caught in Phase 04 smoke; rerun `wrangler secret put` to fix |

## Security Considerations

- All secrets are set via `wrangler secret put` (never in `wrangler.toml`).
- The 401 probe uses a deliberately wrong secret; no sensitive value is logged.
- Run `npm run lint` (existing secret-leak check) before deploy.
