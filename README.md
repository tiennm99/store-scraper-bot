# store-scraper-bot

Telegram bot that tracks Apple App Store and Google Play app version updates.
Sends daily reports to registered groups and alerts when apps haven't been updated
past a configurable threshold. JavaScript (Node.js) port of the original Java
implementation — the earlier Java and Go generations are preserved in this repository's
git history. Runs on Vercel serverless functions with Upstash Redis as the data store.

---

## Features

- **Multi-store tracking** — monitors Apple App Store and Google Play apps in a single bot
- **Multi-group support** — each Telegram group manages its own tracked app list independently
- **Daily reports** — 07:00 Asia/Saigon (00:00 UTC) cron; weekend-silent by default
- **Stale-update warnings** — alerts when an app hasn't shipped an update past N days (configurable per group via `/setdayswarning`)
- **Admin-only commands** — add/remove apps and groups restricted to IDs in `ADMIN_IDS`
- **Upstream response cache** — configurable TTL (default 10 min) reduces scraper API calls
- **ESM, Node 20+** — uses built-in `fetch`; no extra HTTP dependency

---

## Architecture

```
api/
├── webhook.js        # Telegram webhook entry (Vercel function)
└── cron.js           # Daily report cron (Vercel Cron, 00:00 UTC)
src/
├── app-builder.js    # Wires config, Upstash, scrapers, bot, scheduler
├── api/              # apple-scraper.js + google-scraper.js (inlined scrapers)
├── models/           # Plain object factories mirroring original Mongo schema
├── repository/       # Upstash REST adapter + per-collection wrappers
├── bot/
│   ├── commands/     # One file per /command; index.js is single source of truth
│   └── dispatch.js
├── scheduler/        # Cron handler — computes stale apps, formats report
└── util/             # Table renderer, time helpers
scripts/
├── register-webhook.js     # Points Telegram webhook + refreshes menu scopes
└── check-secret-leaks.js   # Pre-commit secret scan
```

Storage uses Upstash Redis keys: `admin`, `group:{chatId}`, `apple:{appId}`,
`google:{appId}`. Key namespace is isolated per deployment via `KEY_PREFIX`.

---

## Requirements

- Node.js 20+
- [Vercel](https://vercel.com) account (Hobby / free tier sufficient)
- [Upstash Redis](https://upstash.com) database (free tier sufficient)
- Telegram bot token from [@BotFather](https://t.me/BotFather)

---

## Quick Start

```sh
git clone https://github.com/tiennm99/store-scraper-bot
cd store-scraper-bot
pnpm install
cp .env.example .env.local
# Fill in TELEGRAM_BOT_TOKEN, UPSTASH_REDIS_REST_URL, UPSTASH_REDIS_REST_TOKEN, etc.
vercel link
vercel env pull .env.local
pnpm dev            # vercel dev — local webhook tunnel
```

---

## Configuration

Set these as Vercel environment variables (or in `.env.local` for local dev):

| Variable | Required | Description |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | Yes | Bot token from @BotFather |
| `TELEGRAM_BOT_USERNAME` | Yes | Bot username (without `@`) |
| `TELEGRAM_WEBHOOK_SECRET` | Yes | ≥32 chars random string; verifies inbound Telegram calls |
| `ADMIN_IDS` | Yes | Comma-separated Telegram user IDs with admin access |
| `UPSTASH_REDIS_REST_URL` | Yes | Upstash REST endpoint (or `KV_REST_API_URL` from Vercel integration) |
| `UPSTASH_REDIS_REST_TOKEN` | Yes | Upstash REST token (or `KV_REST_API_TOKEN` fallback) |
| `KEY_PREFIX` | No | Redis key namespace (default: `store-scraper-bot:`) |
| `CRON_SECRET` | Yes | ≥32 chars random; authenticates Vercel Cron calls |
| `APP_CACHE_SECONDS` | No | Upstream scraper cache TTL in seconds (default: `600`) |
| `NUM_DAYS_WARNING_NOT_UPDATED` | No | Default stale threshold in days (default: `30`) |

Operator-only deploy variables (used by `pnpm register` and `pnpm describe`) go in
`.env.deploy` — see `.env.deploy.example`.

---

## Deploy

```sh
pnpm deploy        # vercel deploy --prod && register webhook + Telegram menu
```

Re-run `pnpm register` any time `src/bot/commands/index.js` changes — Telegram
caches the command menu until `setMyCommands` is called again.

---

## Bot Commands

| Command | Scope | Description |
|---|---|---|
| `/info` | All | Bot info and current group settings |
| `/check_app` | All | Check a specific app's current version |
| `/list_app` | All | List tracked apps in this group |
| `/list_group` | Admin | List all registered groups |
| `/add_group` | Admin | Register a new group |
| `/delete_group` | Admin | Remove a group |
| `/add_apple_app` | Admin | Track an App Store app |
| `/add_google_app` | Admin | Track a Play Store app |
| `/delete_apple_app` | Admin | Remove a tracked App Store app |
| `/delete_google_app` | Admin | Remove a tracked Play Store app |
| `/set_app_ttl` | Admin | Override cache TTL for an app |
| `/setdayswarning` | Admin | Per-group stale-warning threshold |
| `/get_settings` | Admin | Show current group settings |

---

## Operations

**Credential rotation (quarterly):**

- Upstash token — regenerate in Upstash console, update `UPSTASH_REDIS_REST_TOKEN`, redeploy
- Webhook secret — generate new value, update `TELEGRAM_WEBHOOK_SECRET`, redeploy, then `pnpm register`

**Dependency note:** The legacy `app-store-scraper → request` transitive is aliased
to the maintained `@cypress/request` fork via pnpm overrides. Store calls still only
target known endpoints (`itunes.apple.com`, `play.google.com`).

---

## License

Apache 2.0
