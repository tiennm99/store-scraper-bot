#!/usr/bin/env node
// Post-deploy registration: setWebhook (with secret_token) + setMyCommands.
// Run via: npm run register  (reads .env.deploy)
// Dry run via: npm run register:dry

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

await tg('setWebhook', {
  url: URL_,
  secret_token: SECRET,
  allowed_updates: ['message'],
});
await tg('setMyCommands', { commands: COMMANDS });
const info = await tg('getWebhookInfo', {});
console.log('Webhook state:', JSON.stringify(info.result, null, 2));
