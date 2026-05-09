// Daily check entry. Triggered by Vercel Cron at 00:00 UTC = 07:00 Asia/Saigon
// (schedule lives in vercel.json). Validates the Authorization: Bearer
// $CRON_SECRET header to prevent random POSTs from triggering the daily check.
//
// Vercel's `nodejs` runtime passes IncomingMessage/ServerResponse — req.headers
// is a plain object with lowercased keys, not a Web Headers instance.

import { buildApp } from '../src/app-builder.js';
import { runDailyCheck } from '../src/scheduler/scheduler.js';

export const config = { runtime: 'nodejs', maxDuration: 60 };

export default async function handler(req, res) {
  // Fail closed if CRON_SECRET is unset — otherwise the comparison would be
  // `Bearer undefined`, which an attacker could replay as a literal string
  // and bypass auth.
  const expected = process.env.CRON_SECRET;
  const auth = req.headers['authorization'];
  if (!expected || auth !== `Bearer ${expected}`) {
    return res.status(401).send('Unauthorized');
  }

  let app;
  try {
    app = buildApp(process.env);
  } catch (err) {
    console.log(JSON.stringify({ level: 'error', msg: 'config error', err: err.message }));
    return res.status(500).send('Server misconfigured');
  }

  // Cron runs synchronously up to maxDuration; no waitUntil needed.
  await runDailyCheck(app.config, app.store, app.sender, app.appleScraper, app.googleScraper);
  return res.status(200).send('OK');
}
