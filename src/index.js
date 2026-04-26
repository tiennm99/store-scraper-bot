import { loadConfig } from './config.js';
import { createStore } from './repository/store.js';
import { createAppleScraper } from './api/apple-scraper.js';
import { createGoogleScraper } from './api/google-scraper.js';
import { createBot } from './bot/bot.js';
import { dispatch } from './bot/dispatch.js';
import { runDailyCheck } from './scheduler/scheduler.js';

// Builds the per-invocation context. Cheap; relies on memoized MongoClient
// inside the store factory chain.
function build(env) {
  const config = loadConfig(env);
  const store = createStore(env, config.appCacheSeconds);
  const appleScraper = createAppleScraper(config, store);
  const googleScraper = createGoogleScraper(config, store);
  const { sender, commands } = createBot(config, store, appleScraper, googleScraper);
  return { config, store, appleScraper, googleScraper, sender, commands };
}

export default {
  // Telegram webhook entry. Validates the `secret_token` header, acks fast,
  // then dispatches in `ctx.waitUntil` so Telegram doesn't retry on slow Mongo.
  async fetch(request, env, ctx) {
    if (request.method !== 'POST') {
      return new Response('Not found', { status: 404 });
    }

    let app;
    try {
      app = build(env);
    } catch (err) {
      console.log(JSON.stringify({ level: 'error', msg: 'config error', err: err.message }));
      return new Response('Server misconfigured', { status: 500 });
    }

    const secret = request.headers.get('X-Telegram-Bot-Api-Secret-Token');
    if (secret !== app.config.telegramWebhookSecret) {
      return new Response('Unauthorized', { status: 401 });
    }

    let update;
    try {
      update = await request.json();
    } catch {
      return new Response('Bad request', { status: 400 });
    }
    if (!update?.message) return new Response('OK');

    ctx.waitUntil(
      dispatch(update.message, {
        sender: app.sender,
        commands: app.commands,
        config: app.config,
        logger: app.config.logger,
      }),
    );
    return new Response('OK');
  },

  // Daily cron handler. Schedule lives in wrangler.toml.
  async scheduled(event, env, ctx) {
    let app;
    try {
      app = build(env);
    } catch (err) {
      console.log(JSON.stringify({ level: 'error', msg: 'config error', err: err.message }));
      return;
    }
    ctx.waitUntil(
      runDailyCheck(app.config, app.store, app.sender, app.appleScraper, app.googleScraper),
    );
  },
};
