#!/usr/bin/env node
// Sets the bot's profile description (long) + short description.
// Run via: npm run describe  (reads .env.deploy)
// Dry run: npm run describe -- --dry-run

const TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const DRY = process.argv.includes('--dry-run');

if (!TOKEN) {
  console.error('TELEGRAM_BOT_TOKEN is required');
  process.exit(1);
}

const SHORT =
  'Track Apple App Store + Google Play app updates. Get notified when tracked apps go N days without an update.';

const LONG =
  'Tracks Apple App Store and Google Play apps and pings your group when an app has not been updated in over N days.\n\n' +
  'Get started: send /info in your group, ask the bot admin to /addgroup it, then /addapple <id> or /addgoogle <appId>. ' +
  'Use /settings to view per-group config and /setdayswarning <n> to override the warning threshold.';

if (SHORT.length > 120) {
  console.error(`Short description too long (${SHORT.length}/120)`);
  process.exit(1);
}
if (LONG.length > 512) {
  console.error(`Long description too long (${LONG.length}/512)`);
  process.exit(1);
}

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

await tg('setMyShortDescription', { short_description: SHORT });
await tg('setMyDescription', { description: LONG });
const me = await tg('getMe', {});
console.log('Bot:', JSON.stringify(me.result, null, 2));
