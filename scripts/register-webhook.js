#!/usr/bin/env node
// Post-deploy registration:
//   - setWebhook (with secret_token)
//   - setMyCommands at default scope (user-only commands)
//   - setMyCommands at chat scope per ADMIN_ID (full set, including admin commands)
// Run via: npm run register   (reads .env.deploy)
// Dry run via: npm run register:dry

import {
  TELEGRAM_USER_COMMANDS,
  TELEGRAM_ADMIN_COMMANDS,
} from '../src/bot/commands/index.js';
import { parseAdminIds } from '../src/util/parse-admin-ids.js';

const TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const SECRET = process.env.TELEGRAM_WEBHOOK_SECRET;
const URL_ = process.env.WORKER_URL;
const ADMIN_IDS_RAW = process.env.ADMIN_IDS;
const DRY = process.argv.includes('--dry-run');

for (const [k, v] of Object.entries({
  TELEGRAM_BOT_TOKEN: TOKEN,
  TELEGRAM_WEBHOOK_SECRET: SECRET,
  WORKER_URL: URL_,
  ADMIN_IDS: ADMIN_IDS_RAW,
})) {
  if (!v) {
    console.error(`${k} is required`);
    process.exit(1);
  }
}

const adminIds = parseAdminIds(ADMIN_IDS_RAW);
if (adminIds.length === 0) {
  console.error('ADMIN_IDS must contain at least one numeric Telegram user ID');
  process.exit(1);
}

async function tg(method, payload, { allowFail = false } = {}) {
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
    if (allowFail) return body;
    console.error(`${method} failed`, body);
    process.exit(1);
  }
  return body;
}

await tg('setWebhook', {
  url: URL_,
  secret_token: SECRET,
  allowed_updates: ['message'],
});

// Default scope: every chat sees these unless a more specific scope overrides.
await tg('setMyCommands', { commands: TELEGRAM_USER_COMMANDS });

// Per-admin chat scope: full set including admin commands.
// Telegram requires the admin has DMed the bot at least once for `chat` scope
// to resolve. If they haven't, the call returns "chat not found" — log + skip
// rather than aborting the whole register flow.
for (const adminId of adminIds) {
  const result = await tg(
    'setMyCommands',
    {
      commands: TELEGRAM_ADMIN_COMMANDS,
      scope: { type: 'chat', chat_id: adminId },
    },
    { allowFail: true },
  );
  if (!result.ok) {
    console.warn(
      `[warn] setMyCommands for admin ${adminId} failed: ${result.description}. ` +
        `Admin must DM the bot at least once for chat-scoped menu.`,
    );
  }
}

const info = await tg('getWebhookInfo', {});
console.log('Webhook state:', JSON.stringify(info.result, null, 2));
