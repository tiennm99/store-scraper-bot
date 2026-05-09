// Telegram webhook entry. Vercel serverless function (classic Node runtime).
// Validates the X-Telegram-Bot-Api-Secret-Token header, acks fast, then
// dispatches in waitUntil so Telegram doesn't retry on slow downstream calls.
//
// Vercel's `nodejs` runtime passes IncomingMessage/ServerResponse with
// `shouldAddHelpers: true` (so req.body is auto-parsed JSON and res has
// .status/.send helpers). req.headers is a plain object — header keys are
// lowercased.

import { waitUntil } from '@vercel/functions';
import { buildApp } from '../src/app-builder.js';
import { dispatch } from '../src/bot/dispatch.js';

export const config = { runtime: 'nodejs' };

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(404).send('Not found');
  }

  let app;
  try {
    app = buildApp(process.env);
  } catch (err) {
    console.log(JSON.stringify({ level: 'error', msg: 'config error', err: err.message }));
    return res.status(500).send('Server misconfigured');
  }

  const secret = req.headers['x-telegram-bot-api-secret-token'];
  if (secret !== app.config.telegramWebhookSecret) {
    return res.status(401).send('Unauthorized');
  }

  // req.body is auto-parsed by Vercel helpers when Content-Type is JSON.
  // Falsy / non-object guards mirror the prior CF handler.
  const update = req.body;
  if (!update || typeof update !== 'object' || !update.message) {
    return res.status(200).send('OK');
  }

  waitUntil(
    dispatch(update.message, {
      sender: app.sender,
      commands: app.commands,
      config: app.config,
      logger: app.config.logger,
    }),
  );
  return res.status(200).send('OK');
}
