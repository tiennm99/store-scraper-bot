# Xia: store-scraper-bot → js-store-scraper-bot

Mode: `--compare` (defaulted from `--port` after Phase 4 found 0 gaps)

## Source Manifest

- Repo: `/config/workspace/tiennm99/store-scraper-bot` (local)
- Stack: Java 21 + Lombok + Gradle, MongoDB sync driver, Telegrambots long-polling
- Entry: `Main.java` → JVM process
- 47 Java files, ~2021 LoC
- Branch/SHA: not captured (local clone, no git probe)

## Local Manifest

- Path: `/config/workspace/tiennm99/js-store-scraper-bot`
- Stack: Node 20 ESM, Cloudflare Workers (`nodejs_compat_v2`), MongoDB driver v6, Telegram webhook
- Entry: `src/index.js` (`fetch` + `scheduled` handlers)
- ~35 JS files
- Wrangler cron: `0 0 * * *` (UTC = 07:00 ICT)

## Source Anatomy

```
java                              js
├── Main.java                     ├── index.js               (Workers fetch + scheduled)
├── bot/                          ├── bot/
│   ├── StoreScrapeBot            │   ├── bot.js
│   ├── StoreScrapeBotTelegramClient    │   ├── telegram-api.js
│   ├── StoreScrapeBotUsernameSupplier  │   └── (config.telegramBotUsername)
│   ├── command/ × 13             │   ├── dispatch.js
│   ├── entity/NonUpdatedApp      │   └── commands/ × 13 + command-utils.js
│   └── table/Table               │
├── api/{apple,google}            ├── api/
│   ├── *Scraper                  │   ├── apple-scraper.js
│   ├── request/*                 │   └── google-scraper.js
│   └── response/*                │
├── model/                        ├── models/
│   ├── AbstractModel             │   (plain factory functions)
│   ├── Admin / Group             │   ├── admin.js / group.js
│   ├── AppleApp / GoogleApp      │   ├── apple-app.js / google-app.js
│   └── entity/{Apple,Google}AppInfo    │   (embedded in group.appleApps[])
├── repository/                   ├── repository/
│   ├── Abstract*Repository       │   (inlined per-repo)
│   ├── AdminRepository           │   ├── admin-repository.js
│   ├── GroupRepository           │   ├── group-repository.js
│   ├── AppleAppRepository        │   ├── apple-app-repository.js
│   ├── GoogleAppRepository       │   ├── google-app-repository.js
│   └── (MongoDBUtil)             │   ├── mongodb.js
│                                 │   └── store.js          (NEW: composite root)
├── util/                         ├── util/
│   ├── Time                      │   ├── time.js
│   ├── GsonUtil                  │   (native JSON)
│   ├── SchedulerUtil             │   ├── scheduler/scheduler.js
│   └── MongoDBUtil               │   (in repository/mongodb.js)
├── env/Environment               ├── config.js
├── constant/Constant             │   (constants in config.js)
├── type/Env                      │   (env string in config.js)
└── (none)                        └── logger.js             (NEW)
```

## Dependency Matrix

| Source layer | Local layer | Status |
|---|---|---|
| `Main` JVM bootstrap | `index.js` Workers handlers | `EXISTS` (different idiom) |
| `bot/command/*` (13 files) | `bot/commands/*.js` (13 files) | `EXISTS` |
| `api/{apple,google}/*Scraper` | `api/*-scraper.js` | `EXISTS` |
| `model/*` | `models/*.js` | `EXISTS` |
| `repository/*` | `repository/*.js` | `EXISTS` |
| `util/Time` | `util/time.js` | `EXISTS` |
| `bot/table/Table` | `util/table.js` | `EXISTS` |
| `env/Environment` + `constant/Constant` | `config.js` | `EXISTS` (collapsed) |
| `util/GsonUtil` | (native `JSON`) | `NEW` (idiomatic replacement) |
| `util/SchedulerUtil` (`ScheduledExecutorService`) | wrangler cron | `NEW` (platform replacement) |
| `repository/AbstractRepository<K,V>` generic | (inlined) | `NEW` (YAGNI elision) |
| (none) | `logger.js` structured JSON | `EXTRA` |
| (none) | `bot/dispatch.js` | `EXTRA` (split out from bot.js) |
| (none) | `repository/store.js` composite | `EXTRA` (DI root) |
| (none) | webhook secret validation | `EXTRA` (required for webhooks) |
| `Time.useMockTime()` | — | `SKIP` (test-only utility) |

## Decision Matrix

| # | Decision | Source way | Local way | Choice | Risk |
|---|---|---|---|---|---|
| 1 | Code port | reference | already done | **no new code** | low |
| 2 | Daily report format | terse | verbose w/ metadata | **keep JS** | low |
| 3 | Repo abstraction | generic base | inlined | **keep JS** | low |
| 4 | Scheduler | `ScheduledExecutorService` | Cloudflare cron | **keep JS** | low |
| 5 | Verification | implicit prod use | none | **add smoke test** | medium |

## Risk Score

- Critical: 0
- Medium: 1 (verification gap — README itself flags "unverified")
- Low overall → proceed

## Decision

**No implementation work required.** Port already at parity. The xia mode resolves to `--compare`.

What's left is **operational**, not portage:

1. **Smoke-test the deployed Worker** — exercise each of the 13 commands against a non-prod chat once, confirm the daily cron payload renders.
2. **Decide canonical authority** — README still says "Java is reference". If JS is now primary, drop that line and remove the warning banner.
3. **Optional alignment** — if strict parity is desired, swap JS daily-report headers to match Java's terse format. (No behavior change; cosmetic only.)

## Detailed Comparison

See: `plans/reports/researcher-260429-1555-java-vs-js-parity.md`

- 47 Java files mapped to 35 JS files
- 11 PARITY · 2 MINOR · 0 GAP · 3 EXTRA
- Schema/timing/cache TTL/HTML formatting all match

## Open Questions

1. Is JS now the primary impl, or is Java still authoritative? Affects README + which side gets future bug fixes.
2. Are the 3 "EXTRA" JS items (`logger.js`, `dispatch.js`, `store.js`) intended as permanent JS additions, or should they be back-ported to Java for parity?
3. Is the daily-report format divergence (terse Java vs verbose JS) intentional? If yes, document; if no, pick one.
4. Any plan to add a test runner to JS (Jest/Vitest)? README admits unverified end-to-end.
