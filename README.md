# store-scraper-bot

JavaScript (Node.js) implementation. Ports [java-store-scraper-bot](https://github.com/tiennm99/java-store-scraper-bot).
Runs on Vercel serverless functions with Upstash Redis as the data store.

> ⚠️ **Preview / unstable — use at your own risk.**
> This port was produced largely with AI assistance and has **not** been tested
> end-to-end against a live Telegram bot or the upstream Java implementation.
> Behavior parity is intended but unverified. Do not run against a production database.

The Java version remains the reference implementation.

## Status

- Upstash Redis schema mirrors the Java/Go Mongo layout: keys `admin`,
  `group:{chatId}`, `apple:{appId}`, `google:{appId}` (last two TTL'd via Redis
  `EX`). Multi-tenant isolation via `KEY_PREFIX` (default `store-scraper-bot:`).
- Telegram command identifiers match Java exactly: `/info`, `/addgroup`,
  `/delgroup`, `/listgroup`, `/addapple`, `/delapple`, `/addgoogle`,
  `/delgoogle`, `/listapp`, `/checkapp`, `/checkappscore`, `/rawappleapp`,
  `/rawgoogleapp`.
- HTML parse mode; weekend-silent daily report; configurable upstream cache (default 10 min).
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
| `ENV` | `DEVELOPMENT` or `PRODUCTION` |
| `SOURCE_COMMIT` | Optional; shown on startup |
| `APP_CACHE_SECONDS` | Cache TTL for upstream API responses (default 600) |
| `NUM_DAYS_WARNING_NOT_UPDATED` | Threshold for daily warning (default 30) |
| `SCHEDULE_CHECK_APP_TIME` | Cron expression in Vietnam timezone (default `0 7 * * *`) |

Operator-only `.env.deploy` (used by `npm run register`) — see `.env.deploy.example`.

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

`npm run register` re-points the Telegram webhook at the URL in `.env.deploy:WORKER_URL`.

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
