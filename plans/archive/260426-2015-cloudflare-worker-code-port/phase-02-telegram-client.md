# Phase 02 — Worker-Native Telegram Client

## Context Links
- Existing client: `src/bot/bot.js:18-66` (`node-telegram-bot-api` wrapper)
- Telegram Bot API: https://core.telegram.org/bots/api#available-methods
- miti99bot uses grammY; we deliberately stay lighter (raw fetch) since js-store-scraper-bot has 13 commands and no grammY-specific features needed.

## Overview
- **Priority:** P0 — Phase 04 entry handler depends on this.
- **Status:** pending
- **Description:** Replace `node-telegram-bot-api` (which uses Node-only `request`/streams) with a thin `fetch`-based client. Same `sender` interface (`sendMessage`, `sendMessageSilent`, `sendDocument`) so command handlers don't change.

## Key Insights
- `node-telegram-bot-api` pulls in heavy Node-only deps (`request`, `bluebird`, `form-data`). Bundle bloat + Workers incompatibility.
- All 13 commands only need 3 methods: `sendMessage`, `sendDocument`, optional `getMe`.
- `sendMessage` is JSON; `sendDocument` needs `multipart/form-data`. Workers `fetch` + `FormData` + `Blob` handle multipart natively (no library).
- HTML parse mode + `disable_web_page_preview` + `disable_notification` are URL-encodable bool/string params.

## Requirements

### Functional
- New module `src/bot/telegram-api.js` exporting `createTelegramApi(token)` returning:
  - `sendMessage(chatId, text, opts?)` → POST `/sendMessage`
  - `sendDocument(chatId, filename, body, opts?)` → POST `/sendDocument` multipart
  - `getMe()` → GET `/getMe` (used in Phase 05 register script)
- All methods return parsed JSON or throw `TelegramApiError` on `!ok`.
- `src/bot/bot.js` `sender` rebuilt over the new client. **Same exported interface** so commands work unchanged.
- HTML parse mode + `disable_web_page_preview: true` defaults match Java/Go parity.
- `sendMessageSilent` adds `disable_notification: true`.

### Non-functional
- No Node-specific imports (`Buffer.from(...)` is fine — `nodejs_compat_v2` provides it).
- Errors logged with `console.warn({chatId, method, error})` — JSON-style for CF observability parsing.

## Architecture

```
src/bot/telegram-api.js     ← raw fetch wrapper
       ▲
       │
src/bot/bot.js              ← createSender(api, logger) builds {sendMessage, sendMessageSilent, sendDocument}
       ▲
       │
src/bot/commands/*.js       ← unchanged; consume sender interface
```

## Related Code Files

### CREATE
- `src/bot/telegram-api.js` (new)

### MODIFY
- `src/bot/bot.js` — strip `node-telegram-bot-api` import + polling code; expose `createSender` only. Dispatcher logic moves to `src/bot/dispatch.js` in Phase 04.
- `package.json` — `npm uninstall node-telegram-bot-api`.

### DELETE
- (none — the file is rewritten, not removed)

## Implementation Steps

1. **Write `src/bot/telegram-api.js`**:
   ```js
   const TELEGRAM_BASE = 'https://api.telegram.org';

   export class TelegramApiError extends Error {
     constructor(method, status, body) {
       super(`telegram ${method} failed: ${status} ${body}`);
       this.method = method; this.status = status; this.body = body;
     }
   }

   export function createTelegramApi(token) {
     const base = `${TELEGRAM_BASE}/bot${token}`;

     async function callJson(method, payload) {
       const res = await fetch(`${base}/${method}`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify(payload),
       });
       const text = await res.text();
       if (!res.ok) throw new TelegramApiError(method, res.status, text);
       return JSON.parse(text);
     }

     async function callMultipart(method, fields, file) {
       const form = new FormData();
       for (const [k, v] of Object.entries(fields)) form.set(k, String(v));
       if (file) {
         form.set(file.field, new Blob([file.body], { type: file.contentType }), file.filename);
       }
       const res = await fetch(`${base}/${method}`, { method: 'POST', body: form });
       const text = await res.text();
       if (!res.ok) throw new TelegramApiError(method, res.status, text);
       return JSON.parse(text);
     }

     return {
       getMe: () => callJson('getMe', {}),
       sendMessage: (chatId, text, opts = {}) =>
         callJson('sendMessage', { chat_id: chatId, text, ...opts }),
       sendDocument: (chatId, filename, body, opts = {}) =>
         callMultipart('sendDocument', { chat_id: chatId, ...opts },
           { field: 'document', filename, body, contentType: 'application/json' }),
     };
   }
   ```

2. **Rewrite `src/bot/bot.js`** — keep only `createSender` factory and the command map. Drop polling, drop `node-telegram-bot-api`. Pseudocode:
   ```js
   import { createTelegramApi } from './telegram-api.js';
   import { createInfoCommand } from './commands/info.js';
   /* ...other commands... */

   const PARSE_MODE = 'HTML';

   export function createBot(config, appleScraper, googleScraper) {
     const api = createTelegramApi(config.telegramBotToken);
     const logger = config.logger;

     const sender = {
       async sendMessage(chatId, html) {
         try {
           await api.sendMessage(chatId, html, {
             parse_mode: PARSE_MODE,
             disable_web_page_preview: true,
           });
         } catch (err) {
           logger.warn({ chatId, err: err.message }, 'send message failed');
         }
       },
       async sendMessageSilent(chatId, html) { /* + disable_notification: true */ },
       async sendDocument(chatId, filename, body) {
         try { await api.sendDocument(chatId, filename, body); }
         catch (err) { logger.warn({ chatId, err: err.message }, 'send document failed'); }
       },
     };

     const commands = { info: createInfoCommand(), /* ... */ };
     return { sender, commands, api };
   }
   ```
   - Remove `tg.on('message', ...)`, `tg.on('polling_error', ...)`, `tg.getMe()` calls.

3. **Uninstall `node-telegram-bot-api`**:
   ```sh
   npm uninstall node-telegram-bot-api
   ```

4. **Sanity check**: `node --check src/bot/bot.js src/bot/telegram-api.js`.

## Todo List
- [ ] `src/bot/telegram-api.js` written
- [ ] `src/bot/bot.js` rewritten (no polling, no `node-telegram-bot-api`)
- [ ] `node-telegram-bot-api` uninstalled
- [ ] Syntax check passes for both files
- [ ] All 13 commands still import + reference `sender` interface (no breakage)

## Success Criteria
- `node --check` passes for `src/bot/*.js`.
- Sender interface matches existing shape; no command file needs editing.
- Bundle no longer pulls Node-only Telegram lib (verified in Phase 06 deploy).

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `FormData`/`Blob` semantics differ in Workers vs Node | L | Medium | Both follow WHATWG Fetch standard; `nodejs_compat_v2` covers gaps |
| `sendDocument` multipart filename encoding | L | Low | Filename uses ASCII chars only (`{appId}.json`) |
| Telegram rate limits | L | Low | Bot is low-volume; 1 req per command |

## Security Considerations
- Token is in URL path (`/bot{token}/{method}`). Workers `fetch` log doesn't capture request URLs by default. Verify in Observability after deploy.

## Rollback
1. Restore `src/bot/bot.js` from git.
2. `npm install node-telegram-bot-api@^0.66.0`.
3. Delete `src/bot/telegram-api.js`.

## Next Steps
- **Blocks:** Phase 04 (Worker entry needs `createBot` shape).
- **Unblocks:** Phase 04.
