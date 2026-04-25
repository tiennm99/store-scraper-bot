# go-store-scraper-bot

Go port of [store-scraper-bot](https://github.com/tiennm99/store-scraper-bot).

> ⚠️ **Preview / unstable — use at your own risk.**
> This port was produced largely with AI assistance and has **not** been tested
> end-to-end against a live Telegram bot or the upstream Java implementation.
> Behavior parity is intended but unverified. Expect bugs, schema mismatches,
> or runtime failures. Do not run against a production database.

The Java version remains the reference implementation. This Go version is
under development; the plan is to replace the Java version on the original
repo's `feature/go` branch once it stabilizes.

## Status

- Builds cleanly (`go build ./...`, `go vet ./...`).
- Mongo schema is intended to match Java (collections: `common`, `group`,
  `apple_app`, `google_app`; string `_id`; `class` discriminator field).
- Telegram command identifiers match Java exactly (`/info`, `/addgroup`,
  `/delgroup`, `/listgroup`, `/addapple`, `/delapple`, `/addgoogle`,
  `/delgoogle`, `/listapp`, `/checkapp`, `/checkappscore`, `/rawappleapp`,
  `/rawgoogleapp`).
- HTML parse mode; weekend-silent daily report; 10-minute API cache.

## Configuration

Required env vars (see `.env.example`):

| Name | Notes |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Telegram bot token |
| `TELEGRAM_BOT_USERNAME` | Bot username |
| `MONGODB_CONNECTION_STRING` | Preferred (Java parity); falls back to `MONGO_URI` |
| `ADMIN_IDS` | Comma-separated Telegram user IDs |
| `ENV` | `DEVELOPMENT` or `PRODUCTION` |
| `SOURCE_COMMIT` | Optional; shown on startup |

## Run

```sh
go build -o bot ./cmd/bot
./bot
```

Or via Docker Compose:

```sh
docker compose up --build
```
