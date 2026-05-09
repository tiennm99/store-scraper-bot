---
phase: 4
title: "HTTP layer (webhook + cron)"
status: pending
priority: P1
effort: 1h
dependencies: [2, 3]
---

# Phase 4: HTTP layer (webhook + cron)

## Overview

Replace the Cloudflare Worker default-export entry (`src/index.js` with `fetch`/`scheduled` handlers) with two Vercel serverless functions: `api/webhook.js` (Telegram webhook) and `api/cron.js` (daily check via Vercel Cron).

## Requirements

- Telegram webhook validates `X-Telegram-Bot-Api-Secret-Token` header (parity with current CF check)
- Webhook acks fast (<2 s), continues work in background via `@vercel/functions` `waitUntil`
- Cron handler runs daily check, returns 200 on success
- Both handlers use `loadConfig(process.env)` instead of CF `env` arg
- Build wires up `createUpstashClient` once per invocation (cheap — no connection)

## Architecture

```
api/
├── webhook.js     POST handler — Telegram webhook
│                  validates secret, calls dispatch(message), waitUntil
└── cron.js        GET handler — Vercel Cron triggers daily
                   validates Authorization: Bearer $CRON_SECRET (Vercel signs cron requests)
                   runs runDailyCheck(...)

src/
├── index.js       ← DELETE (CF default export gone)
├── config.js      ← Modify: loadConfig(env) → loadConfig(process.env or env)
└── (rest unchanged)
```

Vercel cron requests carry `Authorization: Bearer ${process.env.CRON_SECRET}` header — validate to prevent random POSTs from triggering daily check.

## Related Code Files

- Create: `api/webhook.js`
- Create: `api/cron.js`
- Modify: `src/config.js` (env source)
- Delete: `src/index.js`

## Implementation Steps

1. Update `src/config.js` to take a plain object (works with both `process.env` and CF `env`):
   ```js
   export function loadConfig(env) {
     // existing logic, but `env` is now `process.env` on Vercel
   }
   ```
2. Create `api/webhook.js`:
   ```js
   import { waitUntil } from '@vercel/functions';
   import { loadConfig } from '../src/config.js';
   import { createUpstashClient } from '../src/repository/upstash.js';
   import { createStore } from '../src/repository/store.js';
   import { createAppleScraper } from '../src/api/apple-scraper.js';
   import { createGoogleScraper } from '../src/api/google-scraper.js';
   import { createBot } from '../src/bot/bot.js';
   import { dispatch } from '../src/bot/dispatch.js';

   function build() {
     const config = loadConfig(process.env);
     const client = createUpstashClient(process.env);
     const store = createStore(client, config.appCacheSeconds);
     const appleScraper = createAppleScraper(config, store);
     const googleScraper = createGoogleScraper(config, store);
     const { sender, commands } = createBot(config, store, appleScraper, googleScraper);
     return { config, store, sender, commands };
   }

   export default async function handler(req) {
     if (req.method !== 'POST') return new Response('Not found', { status: 404 });

     const app = build();
     const secret = req.headers.get('X-Telegram-Bot-Api-Secret-Token');
     if (secret !== app.config.telegramWebhookSecret) {
       return new Response('Unauthorized', { status: 401 });
     }
     let update;
     try { update = await req.json(); } catch { return new Response('Bad request', { status: 400 }); }
     if (!update?.message) return new Response('OK');

     waitUntil(dispatch(update.message, {
       sender: app.sender, commands: app.commands,
       config: app.config, logger: app.config.logger,
     }));
     return new Response('OK');
   }

   export const config = { runtime: 'nodejs' };
   ```
3. Create `api/cron.js`:
   ```js
   import { runDailyCheck } from '../src/scheduler/scheduler.js';
   // (same build() helper — extract to src/app-builder.js if duplication bothers)

   export default async function handler(req) {
     const auth = req.headers.get('Authorization');
     if (auth !== `Bearer ${process.env.CRON_SECRET}`) {
       return new Response('Unauthorized', { status: 401 });
     }
     const app = build();
     await runDailyCheck(app.config, app.store, app.sender, app.appleScraper, app.googleScraper);
     return new Response('OK');
   }

   export const config = { runtime: 'nodejs', maxDuration: 60 };
   ```
4. Extract shared `build()` into `src/app-builder.js` (DRY); both functions import.
5. Delete `src/index.js`.
6. Local test: `vercel dev` → `curl -X POST localhost:3000/api/webhook` (expect 401), then with valid secret + JSON body (expect 200).

## Success Criteria

- [ ] `api/webhook.js` and `api/cron.js` exist; both exported as default async handlers
- [ ] Both validate auth (Telegram secret / Cron bearer)
- [ ] `vercel dev` starts without error
- [ ] `src/index.js` deleted
- [ ] `src/config.js` works with `process.env`

## Risk Assessment

- **Risk:** `runtime: 'nodejs'` defaults vary across Vercel CLI versions; some default to edge. **Mitigation:** explicit declaration in function `export const config`.
- **Risk:** `waitUntil` requires Vercel runtime; behaves no-op in `vercel dev`. **Mitigation:** acceptable; production deploy honors it. Verify in Phase 6.
- **Risk:** Cold start on first webhook invocation reaches ~500 ms while Telegram retries on >30 s. **Mitigation:** comfortable margin. Pre-warm not needed.
- **Risk:** `CRON_SECRET` mishandling makes cron callable by anyone. **Mitigation:** generate ≥32-char secret, store as Vercel env var, set on Vercel Cron settings (Vercel injects automatically).
