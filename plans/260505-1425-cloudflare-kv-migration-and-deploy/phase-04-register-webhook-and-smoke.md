---
phase: 4
title: "Register webhook + smoke-test"
status: pending
priority: P1
effort: "1.5h"
dependencies: [3]
---

# Phase 04: Register webhook + smoke-test

## Overview

Point the Telegram Bot API at the deployed Worker, then exercise every command and the daily cron path against the live deployment.

## Requirements

**Functional**
- Telegram delivers updates to `https://<worker-url>` with the secret header.
- All 13 commands return correct responses.
- KV reads/writes show up in `wrangler kv key list --binding STORE_KV`.

**Non-functional**
- Each command response < 1s (Telegram's webhook timeout is 30s, but slow webhooks lead to retries).

## Architecture

Telegram webhook flow:

```
Telegram → POST https://js-store-scraper-bot.<acct>.workers.dev
            with header X-Telegram-Bot-Api-Secret-Token = <secret>
            → Worker validates header (401 on mismatch)
            → Worker parses update, ctx.waitUntil(dispatch(...))
            → Worker returns 200 immediately
            → dispatch reads/writes STORE_KV, calls Telegram sendMessage
```

The cron schedule (`0 0 * * *` UTC = 7am `Asia/Ho_Chi_Minh`) triggers `scheduled()` daily; we manually trigger it once during smoke via `wrangler triggers cron`.

## Related Code Files

**No code changes.** Smoke uses the existing `scripts/register-webhook.js` and live commands.

## Implementation Steps

1. **Populate `.env.deploy`** from `.env.deploy.example`:
   ```
   TELEGRAM_BOT_TOKEN=...
   TELEGRAM_WEBHOOK_SECRET=...   # must match the secret set in Phase 03
   WORKER_URL=https://js-store-scraper-bot.<acct>.workers.dev
   ```

2. **Dry-run register**:
   ```sh
   npm run register:dry
   ```
   Confirms the request body is sane.

3. **Real register**:
   ```sh
   npm run register
   ```
   Calls `setWebhook` + `setMyCommands`. Expect `ok: true` from Telegram.

4. **Verify webhook info**:
   ```sh
   curl -s "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/getWebhookInfo" | jq
   ```
   `url` should match, `pending_update_count` should be 0 once cleared, `has_custom_certificate: false`.

5. **Smoke test (operator runs in Telegram)**:

   | Command | Expected | KV mutation |
   |---|---|---|
   | `/info` | bot info text (admin id, build time) | none |
   | `/addgroup` (in target chat as admin) | "Group added" | `admin` ← `groups` includes chat id |
   | `/listgroup` | shows new group | none |
   | `/addapple <appId> <country>` | "Added" | `group:{chatId}` updated |
   | `/addgoogle <appId> <country>` | "Added" | `group:{chatId}` updated |
   | `/listapp` | renders both lists | none |
   | `/checkapp` | table with ✅/❌ marks | populates `apple:{appId}` / `google:{appId}` |
   | `/checkappscore` | scores table | uses cache from previous step |
   | `/rawappleapp <appId>` | raw JSON dump | reads `apple:{appId}` |
   | `/rawgoogleapp <appId>` | raw JSON dump | reads `google:{appId}` |
   | `/delapple <appId>` | "Removed" | `group:{chatId}` updated |
   | `/delgoogle <appId>` | "Removed" | `group:{chatId}` updated |
   | `/delgroup` | "Group removed" | `admin.groups` shrinks |

6. **Inspect KV** between steps:
   ```sh
   npx wrangler kv key list --binding STORE_KV --remote | jq
   npx wrangler kv key get --binding STORE_KV --remote admin | jq
   ```

7. **Trigger cron manually**:
   ```sh
   npx wrangler triggers cron "0 0 * * *"
   ```
   (Or use the dashboard's "Trigger" button.) Re-add a test app with a known-stale `updated` date (or a real lagging app) and confirm the daily report is posted to the test group.

8. **Cache TTL probe**: read `apple:<appId>` from KV — `expiration` field should be `now + APP_CACHE_SECONDS`. After waiting past TTL, the next `/rawappleapp` should re-fetch from iTunes.

## Todo List

- [ ] Populate `.env.deploy`
- [ ] `npm run register:dry`
- [ ] `npm run register`
- [ ] `getWebhookInfo` reports correct URL
- [ ] Walk through all 13 commands; record responses
- [ ] `wrangler kv key list` shows expected keys after mutations
- [ ] Manually trigger cron; verify daily report message
- [ ] Confirm cache TTL behavior

## Success Criteria

- [ ] All 13 commands return success responses (no "Internal server error").
- [ ] KV inspection matches expected mutations after each command.
- [ ] Manual cron trigger posts a report (or "no stale apps" if none).
- [ ] Worker logs show no unhandled exceptions across the smoke run.

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Webhook secret mismatch → 401 loop | Low | Step 4 verifies `getWebhookInfo`; rotate via `setWebhook` if needed |
| Telegram rate-limited during smoke | Low | Free tier limits are generous for ad-hoc testing; pause if 429 |
| KV eventual-consistency surprises smoke (write then immediate read on different POP) | Low | Same-region writes are visible immediately on modern KV; watch for the rare read-your-write miss and retry |
| `/checkapp` 30s timeout if upstream stores slow | Medium | Both scrapers have own timeouts; if hit, rerun once |

## Security Considerations

- `TELEGRAM_WEBHOOK_SECRET` must be ≥32 chars and unique to this Worker.
- After smoke, verify only authorized admin IDs can issue mutating commands by attempting from a non-admin account → expect "Unauthorized".
- Capture Worker logs from the dashboard and grep for accidental token leaks (none expected; existing structured logger redacts).
