# store-scraper-bot

JavaScript (Node.js) implementation. Ports [java-store-scraper-bot](https://github.com/tiennm99/java-store-scraper-bot).
Runs on Vercel serverless functions with Upstash Redis as the data store.

## Status

- Upstash Redis schema mirrors the Java/Go Mongo layout: keys `admin`,
  `group:{chatId}`, `apple:{appId}`, `google:{appId}` (last two TTL'd via Redis
  `EX`). Multi-tenant isolation via `KEY_PREFIX` (default `store-scraper-bot:`).
- Command set defined in `src/bot/commands/index.js` (single source of truth — catalog drives both dispatch and the Telegram menu). Admin-only commands are hidden from the default menu and shown only in per-admin chat scope.
- HTML parse mode; weekend-silent daily report; configurable upstream cache (default 10 min).
- Per-group warning threshold override via `/setdayswarning` (falls back to `NUM_DAYS_WARNING_NOT_UPDATED` env default).
- Inlined `app-store-scraper` + `google-play-scraper` (no external scraper service).

## Requirements

- Node.js 20+ (uses built-in `fetch`)
- Vercel account (Hobby plan / free tier is enough)
- Upstash Redis database (free tier; sign up at upstash.com or via Vercel Marketplace)

## Configuration

Vercel env vars:

| Name | Notes |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Telegram bot token (required) |
| `TELEGRAM_BOT_USERNAME` | Bot username (required) |
| `TELEGRAM_WEBHOOK_SECRET` | ≥32 chars random; verifies inbound webhook calls |
| `ADMIN_IDS` | Comma-separated Telegram user IDs (required) |
| `UPSTASH_REDIS_REST_URL` | Upstash REST endpoint (or `KV_REST_API_URL` from Vercel Marketplace integration) |
| `UPSTASH_REDIS_REST_TOKEN` | Upstash REST token (or `KV_REST_API_TOKEN` fallback) |
| `KEY_PREFIX` | Namespace for all Redis keys (default `store-scraper-bot:`) |
| `CRON_SECRET` | ≥32 chars random; required by Vercel Cron handler |
| `APP_CACHE_SECONDS` | Cache TTL for upstream API responses (default 600) |
| `NUM_DAYS_WARNING_NOT_UPDATED` | Default warning threshold in days (default 30; per-group override via `/setdayswarning`) |

Operator-only `.env.deploy` (used by `npm run register` + `npm run describe`) — see `.env.deploy.example`.

## Run

Local dev:

```sh
npm install
vercel link            # link to your Vercel project
vercel env pull .env.local
npm run dev            # vercel dev
```

Deploy:

```sh
npm run deploy         # vercel deploy --prod && register webhook
```

`npm run register` re-points the Telegram webhook at the URL in `.env.deploy:WORKER_URL`,
and refreshes the menu: default scope = user commands only, plus a chat-scoped menu
(full set including admin commands) for every ID in `.env.deploy:ADMIN_IDS`. Re-run it
whenever `src/bot/commands/index.js` changes — Telegram caches the menu until
`setMyCommands` is called again. `npm run deploy` does this automatically.
`npm run describe` updates the bot's profile description / about-text (run once when copy changes).

## Operations

### Dashboards

- **Vercel project** — function logs, cron history, deploy status
- **Upstash console** — Redis metrics, key browser, request latency

### Credential rotation (quarterly)

- **Upstash REST token** — regenerate in Upstash console, update `UPSTASH_REDIS_REST_TOKEN` in Vercel env, redeploy
- **Telegram webhook secret** — generate new value, update `TELEGRAM_WEBHOOK_SECRET` in Vercel env, redeploy, then `npm run register`

### Dependency security

- Transitive vulnerabilities from `app-store-scraper → request` are pinned via `overrides` in `package.json` (`form-data`, `qs`, `tough-cookie`).
- The unfixable `request` SSRF advisory is risk-accepted: only known endpoints (`itunes.apple.com`, `play.google.com`) are called; no user-controlled URLs reach `request`.

## Project Layout

```
api/
├── webhook.js          # Telegram webhook entry (Vercel function)
└── cron.js             # Daily cron entry (Vercel Cron)
src/
├── app-builder.js      # wires config, Upstash, scrapers, bot, scheduler
├── config.js
├── logger.js
├── api/
│   ├── apple-scraper.js
│   └── google-scraper.js
├── models/             # plain object factories matching the Mongo schema
├── repository/         # Upstash adapter + per-collection wrappers
├── bot/
│   ├── bot.js          # command dispatch, sender
│   ├── dispatch.js
│   ├── telegram-api.js
│   └── commands/       # one file per /command
├── scheduler/scheduler.js  # 07:00 Asia/Saigon = 00:00 UTC
└── util/               # table renderer, time helpers
scripts/
├── register-webhook.js
└── check-secret-leaks.js
```

## Differences vs Go / Java

- Group / admin / chat IDs are JS `number`s. Telegram chat IDs fit in safe-int
  range, so this is intentional and matches Telegram's documented limits.
- Pino-style structured JSON logging instead of Java/Go's structured loggers.
- HTTP via Node 20's built-in `fetch` (no extra dependency).
- Storage is Upstash Redis (REST) instead of MongoDB; key namespace mirrors the
  original collections, TTL via Redis `EX`.
