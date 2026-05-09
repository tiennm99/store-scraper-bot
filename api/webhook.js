// Telegram webhook entry. Vercel serverless function — replaces the prior
// Cloudflare Worker `fetch` handler. Validates the X-Telegram-Bot-Api-Secret-Token
// header, acks fast, then dispatches in waitUntil so Telegram doesn't retry on
// slow downstream calls.

import { waitUntil } from '@vercel/functions';
import { buildApp } from '../src/app-builder.js';
import { dispatch } from '../src/bot/dispatch.js';

export const config = { runtime: 'nodejs' };

export default async function handler(req) {
  if (req.method !== 'POST') {
    return new Response('Not found', { status: 404 });
  }

  let app;
  try {
    app = buildApp(process.env);
  } catch (err) {
    console.log(JSON.stringify({ level: 'error', msg: 'config error', err: err.message }));
    return new Response('Server misconfigured', { status: 500 });
  }

  const secret = req.headers.get('x-telegram-bot-api-secret-token');
  if (secret !== app.config.telegramWebhookSecret) {
    return new Response('Unauthorized', { status: 401 });
  }

  let update;
  try {
    update = await req.json();
  } catch {
    return new Response('Bad request', { status: 400 });
  }
  if (!update?.message) return new Response('OK');

  waitUntil(
    dispatch(update.message, {
      sender: app.sender,
      commands: app.commands,
      config: app.config,
      logger: app.config.logger,
    }),
  );
  return new Response('OK');
}
