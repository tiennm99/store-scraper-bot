# Phase 05 — Webhook Register Script (Write Only)

## Context Links
- Telegram `setWebhook`: https://core.telegram.org/bots/api#setwebhook
- miti99bot register pattern: `/config/workspace/tiennm99/miti99bot/scripts/register.js`

## Overview
- **Priority:** P1
- **Status:** pending
- **Description:** Write `scripts/register-webhook.js`. **Do not run it** — that's the deploy plan's job. This phase only ensures the script exists and `--dry-run` works locally.

## Key Insights
- The dry-run path doesn't call Telegram, so it's safe to invoke during code-only work.
- `--env-file=.env.deploy` requires Node ≥20.6; project already requires Node ≥20.

## Requirements

### Functional
- `scripts/register-webhook.js` written. Reads `TELEGRAM_BOT_TOKEN`, `TELEGRAM_WEBHOOK_SECRET`, `WORKER_URL` from env.
- `--dry-run` prints `setWebhook` + `setMyCommands` payloads without HTTP calls.
- 13-command list embedded with descriptions.

### Non-functional
- Script logs Telegram response; never prints token.
- Exit 1 on missing env or non-`ok` response.

## Related Code Files

### CREATE
- `scripts/register-webhook.js`

### MODIFY
- (none — `package.json` already has `register` + `register:dry` scripts from Phase 01)

## Implementation Steps

1. **Write `scripts/register-webhook.js`**:
   ```js
   #!/usr/bin/env node
   const TOKEN = process.env.TELEGRAM_BOT_TOKEN;
   const SECRET = process.env.TELEGRAM_WEBHOOK_SECRET;
   const URL_ = process.env.WORKER_URL;
   const DRY = process.argv.includes('--dry-run');

   for (const [k, v] of Object.entries({
     TELEGRAM_BOT_TOKEN: TOKEN,
     TELEGRAM_WEBHOOK_SECRET: SECRET,
     WORKER_URL: URL_,
   })) {
     if (!v) {
       console.error(`${k} is required`);
       process.exit(1);
     }
   }

   const COMMANDS = [
     { command: 'info', description: 'Show this group ID' },
     { command: 'addgroup', description: '[admin] Authorize a group' },
     { command: 'delgroup', description: '[admin] Deauthorize a group' },
     { command: 'listgroup', description: '[admin] List authorized groups' },
     { command: 'addapple', description: 'Track an Apple App Store app' },
     { command: 'delapple', description: 'Stop tracking an Apple app' },
     { command: 'addgoogle', description: 'Track a Google Play app' },
     { command: 'delgoogle', description: 'Stop tracking a Google app' },
     { command: 'listapp', description: 'List tracked apps in this group' },
     { command: 'checkapp', description: 'Check update status of tracked apps' },
     { command: 'checkappscore', description: 'Check scores + ratings of tracked apps' },
     { command: 'rawappleapp', description: 'Dump raw Apple API JSON for an app' },
     { command: 'rawgoogleapp', description: 'Dump raw Google API JSON for an app' },
   ];

   async function tg(method, payload) {
     if (DRY) {
       console.log(`[dry-run] ${method}`, JSON.stringify(payload, null, 2));
       return { ok: true, result: '(dry)' };
     }
     const res = await fetch(`https://api.telegram.org/bot${TOKEN}/${method}`, {
       method: 'POST',
       headers: { 'Content-Type': 'application/json' },
       body: JSON.stringify(payload),
     });
     const body = await res.json();
     if (!body.ok) {
       console.error(`${method} failed`, body);
       process.exit(1);
     }
     return body;
   }

   await tg('setWebhook', { url: URL_, secret_token: SECRET, allowed_updates: ['message'] });
   await tg('setMyCommands', { commands: COMMANDS });
   const info = await tg('getWebhookInfo', {});
   console.log('Webhook state:', JSON.stringify(info.result, null, 2));
   ```

2. **Local dry-run smoke** (using fake env):
   ```sh
   TELEGRAM_BOT_TOKEN=fake TELEGRAM_WEBHOOK_SECRET=fake WORKER_URL=https://example.test \
     node scripts/register-webhook.js --dry-run
   ```
   Expect both payloads printed, exit 0.

## Todo List
- [ ] `scripts/register-webhook.js` written
- [ ] Local dry-run smoke passes
- [ ] `node --check scripts/register-webhook.js` passes

## Success Criteria
- Script runs in dry-run mode without env file (env injected on the command line).
- `getWebhookInfo` is the third call, after `setWebhook` and `setMyCommands`.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Top-level await rejected by older Node | L | Low | Engines `>=20`; top-level await fine |
| Command list drifts from `src/bot/bot.js` map | M | Medium | Both list 13 commands; cross-reference at smoke |

## Next Steps
- **Blocks:** Deploy plan Phase 03 (uses this script).
- **Unblocks:** end of code plan.
