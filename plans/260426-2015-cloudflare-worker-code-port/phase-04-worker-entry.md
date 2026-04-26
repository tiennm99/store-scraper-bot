# Phase 04 — Worker Entry: fetch + scheduled

## Context Links
- Existing entry: `src/index.js` (Node polling + signal handlers)
- Existing scheduler: `src/scheduler/scheduler.js` (uses `node-cron`)
- CF Workers handler reference: https://developers.cloudflare.com/workers/runtime-apis/handlers/
- Telegram webhook payload: https://core.telegram.org/bots/api#update

## Overview
- **Priority:** P0 — the actual Workers deployment unit.
- **Status:** pending
- **Description:** Replace polling entry + `node-cron` + `process.on` with Worker `export default { fetch, scheduled }`. Webhook validation via `X-Telegram-Bot-Api-Secret-Token` header.

## Key Insights
- Workers entry is `export default` with two handlers; nothing else runs on cold start.
- `fetch(request, env, ctx)` MUST respond within ~30s; scheduled is generous (~5min) but Telegram retries webhook on non-200.
- Ack the webhook **immediately** then process async via `ctx.waitUntil(...)` — keeps Telegram from retrying on slow Mongo.
- `scheduled(event, env, ctx)` runs once per cron tick globally — no isolate uniqueness guarantee, but Telegram-side dedup is not needed for our daily report (idempotent enough).
- `pino` is removed; replace with `console.log({...})` — CF Observability indexes JSON output.
- `node-cron` removed entirely; `wrangler.toml [triggers]` declares cron, Workers fires `scheduled` handler.
- `dotenv` removed; `env` arg is the binding source.

## Requirements

### Functional
- `src/index.js` exports `default { fetch, scheduled }`.
- `fetch`:
  - Accepts only `POST /` (or `POST /webhook`); other paths → 404.
  - Validates `X-Telegram-Bot-Api-Secret-Token` header against `env.TELEGRAM_WEBHOOK_SECRET`. Mismatch → 401.
  - Parses JSON update; extracts `update.message`.
  - Routes to dispatcher; `ctx.waitUntil(dispatch(...))`. Returns `200 OK` immediately.
- `scheduled`:
  - Builds store + scrapers + sender; calls `runDailyCheck()` (logic from existing `scheduler.js`, minus `node-cron` wrapping).
  - All work inside `ctx.waitUntil(...)`.
- `src/bot/dispatch.js` (new) — extracts the per-message dispatch logic from old `bot.js`. Takes `(message, sender, commands, logger)`.
- `src/scheduler/scheduler.js` simplified — exports `runDailyCheck(config, store, sender, appleScraper, googleScraper)` only; remove `node-cron` schedule/start/stop.
- `src/config.js` rewritten — accepts `env` arg, returns config object. No `dotenv`. No `pino`. Logger = thin `console.log` wrapper returning JSON.

### Non-functional
- No `process.on`, no `process.exit`, no signal handling.
- No top-level `await`.
- All async errors logged with `console.error({err: err.message, stack: err.stack, ctx: ...})`.

## Architecture

```
src/index.js
  ├─ default.fetch(request, env, ctx)
  │    ├─ validate secret header
  │    ├─ parse update
  │    └─ ctx.waitUntil(dispatch(message, sender, commands, logger))
  │       ↳ returns Response("OK", 200) immediately
  │
  └─ default.scheduled(event, env, ctx)
       └─ ctx.waitUntil(runDailyCheck(config, store, sender, appleScraper, googleScraper))

src/bot/dispatch.js (NEW)
  └─ async dispatch(message, sender, commands, logger)
       ├─ parseCommandName(message.text, botUsername)
       ├─ commands[name](message, sender)
       └─ catch → sender.sendMessage(chatId, "Internal server error")
```

## Related Code Files

### CREATE
- `src/bot/dispatch.js`

### MODIFY
- `src/index.js` — full rewrite as Worker entry.
- `src/config.js` — accept `env` arg; remove `dotenv`, `pino`.
- `src/logger.js` — replace pino with `createConsoleLogger()` (5 lines: `info/warn/error/debug` → `console.log(JSON.stringify({level, ...payload, msg}))`).
- `src/scheduler/scheduler.js` — strip `node-cron`; export `runDailyCheck` only.
- `src/bot/bot.js` — keep `createBot` factory (sender + commands map); dispatch logic moves to `dispatch.js`.

### DELETE
- (none — entry is rewritten)

### NPM uninstall
- `dotenv`, `pino`, `pino-pretty`, `node-cron`.

## Implementation Steps

1. **Rewrite `src/logger.js`** as `console.log` JSON wrapper:
   ```js
   export function createLogger() {
     const log = (level, payloadOrMsg, maybeMsg) => {
       const payload = typeof payloadOrMsg === 'object' ? payloadOrMsg : {};
       const msg = typeof payloadOrMsg === 'string' ? payloadOrMsg : maybeMsg ?? '';
       console.log(JSON.stringify({ level, ts: new Date().toISOString(), msg, ...payload }));
     };
     return {
       debug: (p, m) => log('debug', p, m),
       info: (p, m) => log('info', p, m),
       warn: (p, m) => log('warn', p, m),
       error: (p, m) => log('error', p, m),
     };
   }
   ```

2. **Rewrite `src/config.js`** to accept `env`:
   ```js
   import { createLogger } from './logger.js';

   export function loadConfig(env) {
     const required = ['TELEGRAM_BOT_TOKEN', 'TELEGRAM_BOT_USERNAME',
                       'TELEGRAM_WEBHOOK_SECRET', 'MONGODB_URI', 'ADMIN_IDS'];
     for (const k of required) if (!env[k]) throw new Error(`${k} is required`);
     const adminIds = env.ADMIN_IDS.split(',').map(s => Number(s.trim())).filter(Number.isFinite);
     return {
       telegramBotToken: env.TELEGRAM_BOT_TOKEN,
       telegramBotUsername: env.TELEGRAM_BOT_USERNAME,
       telegramWebhookSecret: env.TELEGRAM_WEBHOOK_SECRET,
       adminIds,
       isAdmin: (id) => adminIds.includes(Number(id)),
       appCacheSeconds: Number(env.APP_CACHE_SECONDS ?? 600),
       numDaysWarningNotUpdated: Number(env.NUM_DAYS_WARNING_NOT_UPDATED ?? 30),
       timezone: 'Asia/Ho_Chi_Minh',
       logger: createLogger(),
     };
   }
   ```

3. **Write `src/bot/dispatch.js`**:
   ```js
   export async function dispatch(message, { sender, commands, config, logger }) {
     if (!message?.text || message.text[0] !== '/') return;
     const name = parseCommandName(message.text, config.telegramBotUsername);
     if (!name) return;
     const handler = commands[name];
     if (!handler) { logger.debug({ command: name }, 'Unknown command'); return; }
     logger.info({ command: name, userId: message.from?.id, chatId: message.chat.id }, 'Executing command');
     try {
       await handler(message, sender);
     } catch (err) {
       logger.error({ err: err.message, command: name }, 'command failed');
       await sender.sendMessage(message.chat.id, 'Internal server error');
     }
   }

   function parseCommandName(text, botUsername) {
     const space = text.indexOf(' ');
     const head = space < 0 ? text.slice(1) : text.slice(1, space);
     const at = head.indexOf('@');
     if (at < 0) return head;
     const cmd = head.slice(0, at), target = head.slice(at + 1);
     if (botUsername && target && target.toLowerCase() !== botUsername.toLowerCase()) return null;
     return cmd;
   }
   ```

4. **Trim `src/bot/bot.js`** — remove `tg.on(...)`, `tg.getMe()` startup; keep only `createBot(config, store, appleScraper, googleScraper)` returning `{ sender, commands }`.

5. **Trim `src/scheduler/scheduler.js`** — remove `import cron from 'node-cron'`, remove `start()`/`stop()`. Export `runDailyCheck(config, store, sender, appleScraper, googleScraper)` only. Repository calls thread through `store`.

6. **Rewrite `src/index.js`**:
   ```js
   import { loadConfig } from './config.js';
   import { createStore } from './repository/store.js';
   import { createAppleScraper } from './api/apple-scraper.js';
   import { createGoogleScraper } from './api/google-scraper.js';
   import { createBot } from './bot/bot.js';
   import { dispatch } from './bot/dispatch.js';
   import { runDailyCheck } from './scheduler/scheduler.js';

   function build(env) {
     const config = loadConfig(env);
     const store = createStore(env);
     const appleScraper = createAppleScraper(config, store);
     const googleScraper = createGoogleScraper(config, store);
     const { sender, commands } = createBot(config, store, appleScraper, googleScraper);
     return { config, store, appleScraper, googleScraper, sender, commands };
   }

   export default {
     async fetch(request, env, ctx) {
       if (request.method !== 'POST') return new Response('Not found', { status: 404 });
       const ctx_ = build(env);
       const secret = request.headers.get('X-Telegram-Bot-Api-Secret-Token');
       if (secret !== ctx_.config.telegramWebhookSecret) {
         return new Response('Unauthorized', { status: 401 });
       }
       const update = await request.json().catch(() => null);
       if (!update?.message) return new Response('OK');
       ctx.waitUntil(dispatch(update.message, { ...ctx_, logger: ctx_.config.logger }));
       return new Response('OK');
     },
     async scheduled(event, env, ctx) {
       const ctx_ = build(env);
       ctx.waitUntil(
         runDailyCheck(ctx_.config, ctx_.store, ctx_.sender, ctx_.appleScraper, ctx_.googleScraper),
       );
     },
   };
   ```

7. **Audit api/scrapers** — `src/api/apple-scraper.js` + `google-scraper.js` already use `fetch`. They reference `config.logger` only. Adapt: `createAppleScraper(config, store)` so `getCachedAppleApp` is called via `store.appleApp.getCached(...)` instead of importing repo module directly. Same for google.

8. **Uninstall Node-only deps**:
   ```sh
   npm uninstall dotenv pino pino-pretty node-cron
   ```

9. **Local dev test**: `npx wrangler dev` and curl POST a fake update with the secret header; verify dispatch logs.

10. **Sanity check**: `node --check src/**/*.js`.

## Todo List
- [ ] `src/logger.js` rewritten as JSON console wrapper
- [ ] `src/config.js` rewritten to accept `env`
- [ ] `src/bot/dispatch.js` written
- [ ] `src/bot/bot.js` trimmed (no polling)
- [ ] `src/scheduler/scheduler.js` trimmed (no node-cron)
- [ ] `src/index.js` rewritten as Worker entry
- [ ] `src/api/{apple,google}-scraper.js` adapted to take `store`
- [ ] Node-only deps uninstalled
- [ ] `wrangler dev` + curl test passes
- [ ] `node --check src/**/*.js` passes

## Success Criteria
- POST to local `wrangler dev` with valid secret + fake `/info` update → bot replies via real Telegram API.
- POST without secret → 401.
- GET / → 404.
- `wrangler dev --test-scheduled` invocation runs `runDailyCheck` without errors.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `ctx.waitUntil` unhandled rejection silently dropped | M | M | Wrap dispatch + scheduler bodies in try/catch + `logger.error` |
| Webhook ack delayed > 30s → Telegram retries | L | M | Ack first, dispatch in `waitUntil` (already in design) |
| Multiple bot replies on Telegram retry | L | L | Telegram dedupes by `update_id` server-side; fast ack reduces retry trigger |
| Cold-start exceeds 50ms CPU on Free plan | M | High | Phase 01 hard gate caught this; this phase only adds JSON parse overhead (sub-ms) |

## Security Considerations
- `TELEGRAM_WEBHOOK_SECRET`: random ≥32 chars, set via `wrangler secret put`. Validated on every `fetch`.
- 401 returned **before** parsing body (avoids spending CPU on attacker payloads).

## Rollback
1. Restore all modified files from git (`git checkout src/`).
2. Reinstall Node-only deps.
3. Existing Node polling bot resumes if launched separately; this Worker simply isn't deployed.

## Next Steps
- **Blocks:** Phase 05 (webhook registration needs the URL of a deployed `fetch` handler).
- **Unblocks:** Phase 05.
