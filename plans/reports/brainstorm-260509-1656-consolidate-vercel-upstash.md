# Brainstorm — Consolidate js-store-scraper-bot on Vercel + Upstash

**Date:** 2026-05-09
**Branch:** main
**Status:** Decision locked, ready to plan

---

## Problem Statement

`js-store-scraper-bot` (Telegram bot) currently splits across:
- **Cloudflare Workers** (free tier) — bot logic, KV, daily cron. Recently migrated from MongoDB Atlas → CF KV.
- **Vercel** (`tiennm99/store-scraper`) — Express wrapper around `app-store-scraper` + `google-play-scraper` npm libs. Bot calls via HTTP.

User wants single free-tier service, no self-hosting, no vendor lock to CF. Needs to remove the cross-service `fetch()` to `store-scraper.vercel.app`.

---

## Constraints (locked via Q&A)

| Constraint | Value |
|---|---|
| Hosting | Free tier only |
| Architecture | Single service (consolidate) |
| Vendor lock | OK to leave CF, OK to stay on Vercel |
| Self-hosting | No |
| Lib parity needed today | `app()` only on both stores |
| Future parity might want | search / similar / reviews |

---

## Approaches Evaluated

### Path 1 — Do nothing (rejected by user)
Keep CF Worker + Vercel scraper. Architecturally correct, zero effort, both free.
**Rejected:** user wants single service.

### Path 2 — Consolidate on Vercel ← chosen
Move bot onto Vercel as serverless functions + Vercel Cron. Inline scraper libs. Replace CF KV.

### Path 3 — Native fetch on CF Workers (rejected)
Skip libs, hand-roll `app()` in ~100 LOC.
**Rejected:** free-tier 10ms CPU/invocation breaks daily cron parsing N Play HTMLs. Loses lib parity for future features.

---

## Storage Sub-Decision: Upstash vs Atlas

Researched 2026 state. Verdict: **Upstash Redis**.

| Dimension | Upstash | Atlas M0 | Winner |
|---|---|---|---|
| Repo layer change cost | Swap binding (~50 LOC) | Full rewrite back to Mongo doc model | Upstash |
| Persistence | RocksDB + daily snapshots | Canonical DB + backups | Tie |
| Cold-start latency (Vercel) | 20–30 ms | 150–300 ms TCP+TLS+auth | Upstash |
| Bundle size | ~50 KB SDK | ~80 MB driver | Upstash |
| Idle behavior | None | 30-day auto-pause | Upstash |
| REST escape hatch | Native | Atlas Data API EOL Sep 2025 | Upstash |
| Free-tier headroom for 50–200 ops/day | 500k cmd/mo (41× margin) | ∞ but irrelevant | Tie |
| Schema parity with Java/Go bots | n/a | Yes | Atlas (but YAGNI here) |

Atlas only wins if user plans to share data with Java/Go reference impls — not the case.

Persistence concern user raised resolved: Upstash is durable, not in-memory.

**Cache loss risk:** apple/google app cache rebuilds in ≤10 min from upstream APIs. Admin/group state is small + slow-changing; can snapshot to JSON in repo if paranoid.

Source: `plans/reports/researcher-260509-1656-upstash-vs-atlas.md`

---

## Recommended Solution

### Architecture

```
Vercel Project (single deploy, free Hobby plan)
├── api/webhook.js      Telegram webhook entry
│                       Validates X-Telegram-Bot-Api-Secret-Token
│                       waitUntil(dispatch(message, ...))
│                       Returns 200 OK fast
├── api/cron.js         Vercel Cron 0 0 * * * (07:00 Asia/Saigon)
│                       runDailyCheck(...)
└── src/
    ├── api/
    │   ├── apple-scraper.js     Inline → store.app() direct
    │   └── google-scraper.js    Inline → gplay.app() direct
    ├── repository/              Upstash Redis adapter
    ├── bot/                     Unchanged (commands, dispatch, sender)
    └── scheduler/               Unchanged

Removed:
  wrangler.toml, wrangler devDep, STORE_KV binding
  src/index.js (CF default export)
  store-scraper.vercel.app fetch BASE_URL constants
  ctx.waitUntil → @vercel/functions waitUntil
```

### New Dependencies

| Package | Why |
|---|---|
| `app-store-scraper@^0.18.0` | Inlined Apple scraper |
| `google-play-scraper@^10.1.2` | Inlined Google scraper |
| `@upstash/redis` | KV storage |
| `@vercel/functions` | `waitUntil` analog of CF `ctx.waitUntil` |

### Removed

- `wrangler` (devDep)
- `mongodb` (devDep — was used for the Atlas → KV migration; obsolete)

### Storage Schema (Upstash)

Flat key-namespaced KV. Mirrors current CF KV layout:

| Key pattern | Value | TTL |
|---|---|---|
| `admin` | JSON array of Telegram user IDs | none |
| `group:<chatId>` | JSON group config | none |
| `cache:apple:<appId>` | JSON app data | `APP_CACHE_SECONDS` (default 600) |
| `cache:google:<appId>` | JSON app data | `APP_CACHE_SECONDS` |

Iteration via `SCAN MATCH group:*` for daily check.

### Cutover Plan

1. **Hard cutover** — single migration window <5 min:
   - Export CF KV via `wrangler kv key list --remote` + `wrangler kv key get` loop (or use existing `scripts/.atlas-export.json` if still present)
   - Import to Upstash via `@upstash/redis` script
   - Deploy Vercel app with new webhook URL
   - Re-register Telegram webhook to new Vercel URL
   - Verify `/info` command works
2. **Old `store-scraper.vercel.app` deployment** — leave running 1–2 weeks as fallback, then delete.
3. **Old CF Worker** — leave deployed (silent — Telegram webhook no longer points there) for ~1 week, then `wrangler delete`.

---

## Risks / Mitigations

| Risk | Mitigation |
|---|---|
| Vercel Hobby cron limit (1×/day max) | Schedule is daily — fits exactly. |
| Vercel function timeout (10s default, 60s max) | Bump cron handler to `maxDuration: 60`. Daily check loops sequentially over groups; should fit. |
| Telegram webhook >30s = retry storm | Use `@vercel/functions` `waitUntil()` to ack fast and continue work in background. |
| Upstash 256 MB cap eviction | Bot data <100 KB. ~2500× headroom. |
| Lib bundle size | Vercel function limit 250 MB uncompressed; libs ~30 MB. Comfortable. |
| TLS / cert issues with `app-store-scraper` `request` lib in Node 20+ | Node 20 still supports `request`. If breaks, swap to native `fetch()` shim — fallback path. |
| Re-registering Telegram webhook | Existing `scripts/register-webhook.js` reusable; just change URL var. |

---

## Success Metrics

- Telegram `/info` returns within 2 s after Vercel deploy
- Daily cron runs at 07:00 VN time (visible in Vercel Functions log)
- Admin + group state preserved across migration (count match pre/post)
- Apple + Google `/checkapp` returns identical data to current Vercel scraper output
- No lingering calls to `store-scraper.vercel.app` (grep src/ for vercel.app)
- Vercel function bundle <50 MB, cold start <500 ms

---

## Next Steps

1. Generate phased plan via `/ck:plan` (this triggers next).
2. Implementation phases (preview):
   - Phase 1 — Vercel scaffolding (`vercel.json`, env, deps)
   - Phase 2 — Upstash repository adapter
   - Phase 3 — Inline scraper modules (drop fetch BASE_URL)
   - Phase 4 — HTTP layer (`api/webhook.js`, `api/cron.js`)
   - Phase 5 — Data migration script (CF KV → Upstash)
   - Phase 6 — Deploy + cutover + Telegram webhook re-register
   - Phase 7 — Cleanup (remove wrangler, stale scripts, README update)

---

## Unresolved Questions

1. Vercel region preference? Default `iad1` (us-east) is fine; bot is low-latency-tolerant.
2. Should the old `tiennm99/store-scraper` repo be archived/deleted now or after 2-week observation? (User leaning: keep running, mark deprecated.)
3. Re-add Mongo schema docs in `docs/` or remove (since we're permanent KV now)?
4. Do we want Vercel Marketplace Upstash integration (env vars auto-injected) or BYO Upstash account?
