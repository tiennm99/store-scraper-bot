# js-store-scraper-bot

JavaScript (Node.js) port of [store-scraper-bot](https://github.com/tiennm99/store-scraper-bot).

> ⚠️ **Preview / unstable — use at your own risk.**
> This port was produced largely with AI assistance and has **not** been tested
> end-to-end against a live Telegram bot or the upstream Java implementation.
> Behavior parity is intended but unverified. Do not run against a production database.

The Java version remains the reference implementation.

## Status

- Mongo schema matches Java/Go (collections: `common`, `group`, `apple_app`,
  `google_app`; string `_id`; `class` discriminator).
- Telegram command identifiers match Java exactly: `/info`, `/addgroup`,
  `/delgroup`, `/listgroup`, `/addapple`, `/delapple`, `/addgoogle`,
  `/delgoogle`, `/listapp`, `/checkapp`, `/checkappscore`, `/rawappleapp`,
  `/rawgoogleapp`.
- HTML parse mode; weekend-silent daily report; configurable API cache (default 10 min).

## Requirements

- Node.js 20+ (uses built-in `fetch`)
- MongoDB 4.4+

## Configuration

See `.env.example`:

| Name | Notes |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Telegram bot token (required) |
| `TELEGRAM_BOT_USERNAME` | Bot username (required) |
| `MONGODB_CONNECTION_STRING` | Preferred (Java parity); falls back to `MONGO_URI` |
| `MONGO_DATABASE` | Optional; inferred from URI path if omitted |
| `ADMIN_IDS` | Comma-separated Telegram user IDs (required) |
| `ENV` | `DEVELOPMENT` or `PRODUCTION` |
| `SOURCE_COMMIT` | Optional; shown on startup |
| `APP_CACHE_SECONDS` | Cache TTL for upstream API responses (default 600) |
| `NUM_DAYS_WARNING_NOT_UPDATED` | Threshold for daily warning (default 30) |
| `SCHEDULE_CHECK_APP_TIME` | Cron expression in Vietnam timezone (default `0 7 * * *`) |

## Run

```sh
npm install
cp .env.example .env   # then edit credentials
npm start
```

Or via Docker Compose:

```sh
docker compose up --build
```

## Migrating from MongoDB Atlas (one-time)

If you're moving an existing bot off Atlas to Cloudflare KV:

```sh
# 1. Make sure .env has MONGODB_URI pointing at the Atlas cluster (read-only is fine).
npm install                    # pulls the mongodb devDep needed by the migration script
npm run migrate                # writes scripts/.atlas-export.json (admin + groups)
# Optionally also migrate cached app entries with their TTL preserved:
#   npm run migrate -- --include-cache
npm run migrate:bulk           # uploads the JSON to the production KV namespace
rm scripts/.atlas-export.json  # contains your bot state — delete after success
```

Cache collections (`apple_app`, `google_app`) are skipped by default since they auto-rebuild from upstream APIs within `APP_CACHE_SECONDS`. Re-running the migration is idempotent.

## Project Layout

```
src/
├── index.js              # entry point: wire up config, mongo, scrapers, bot, scheduler
├── config.js
├── logger.js
├── api/
│   ├── apple-scraper.js
│   └── google-scraper.js
├── models/               # plain object factories matching Mongo docs
├── repository/           # Mongo collection wrappers (admin / group / cached app)
├── bot/
│   ├── bot.js            # Telegram polling, command dispatch, sender
│   └── commands/         # one file per /command
├── scheduler/scheduler.js  # daily 7am Vietnam-time check
└── util/                 # table renderer, time helpers
```

## Differences vs Go / Java

- Group / admin / chat IDs are JS `number`s. Telegram chat IDs fit in safe-int
  range, so this is intentional and matches Telegram's documented limits.
- Pino logging instead of Java/Go's structured loggers; semantics equivalent.
- HTTP via Node 20's built-in `fetch` (no extra dependency).
