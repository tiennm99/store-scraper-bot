---
phase: 5
title: "Data migration MongoDB Atlas → Upstash"
status: pending
priority: P1
effort: 30 min
dependencies: [2]
---

# Phase 5: Data migration MongoDB Atlas → Upstash

## Overview

One-shot migration: read live state from the legacy `java-store-scraper-bot` MongoDB Atlas (collections `common`, `group`, optional `apple_app`/`google_app`) and write into Upstash Redis with TTL preserved on cache entries. The Java bot remains source of truth until cutover (Phase 6).

CF KV is **not** the source — the Node port never deployed live, so no CF KV state exists. Skipping CF KV simplifies the cutover.

## Requirements

- Migrate `admin` (from `common._id == "admin"`) and `group` collection (must preserve)
- Skip `apple_app`/`google_app` cache by default (auto-rebuilds within `APP_CACHE_SECONDS`); `--include-cache` flag for parity
- Idempotent — safe to re-run; overwrites with same data
- Preserves TTL on cache entries (recompute from `millis` field)
- Read-only against Atlas — Java bot keeps serving until Phase 6 cutover
- **Apply same `KEY_PREFIX` (default `store-scraper-bot:`) as runtime adapter** — physical Upstash keys must match what Phase 2 adapter reads. Mismatch ⇒ migrated data invisible to bot.

## Architecture

Adapt existing `scripts/migrate-atlas-to-kv.js`. Replace the JSON-file output + `wrangler bulk put` step with direct Upstash REST writes via `@upstash/redis`:

```
scripts/migrate-atlas-to-upstash.js
  PREFIX = process.env.KEY_PREFIX ?? 'store-scraper-bot:'
  1. MongoClient(MONGODB_URI) — connects to legacy Atlas (read-only)
  2. Read common.findOne({_id:'admin'}) → SET PREFIX+admin <json>
  3. Read group.find({}) → for each: SET PREFIX+group:<_id> <json>
  4. If --include-cache:
       Read apple_app.find({}) → SET PREFIX+apple:<_id> <json> EX <ttl-from-millis>
       Read google_app.find({}) → SET PREFIX+google:<_id> <json> EX <ttl-from-millis>
  5. Log counts: admin, groups, apple (kept/skipped), google (kept/skipped)
  6. Log effective PREFIX so operator can spot mismatches early
```

Reuses `remainingTtl()` logic from existing migrate script. No on-disk JSON intermediate — direct Atlas → Upstash, so bot state never touches the filesystem.

## Related Code Files

- Create: `scripts/migrate-atlas-to-upstash.js` (adapt from `scripts/migrate-atlas-to-kv.js`)
- Delete: `scripts/migrate-atlas-to-kv.js` (CF KV path obsolete)
- Modify: `package.json`
  - Replace `"migrate"` script: `node --env-file=.env.deploy scripts/migrate-atlas-to-upstash.js`
  - Drop `"migrate:bulk"` (CF-specific)

## Implementation Steps

1. Copy `scripts/migrate-atlas-to-kv.js` → `scripts/migrate-atlas-to-upstash.js`.
2. Replace JSON-array assembly + `writeFile` with Upstash writes (applying `KEY_PREFIX`):
   ```js
   import { Redis } from '@upstash/redis';
   const redis = new Redis({
     url: process.env.UPSTASH_REDIS_REST_URL,
     token: process.env.UPSTASH_REDIS_REST_TOKEN,
   });
   const PREFIX = process.env.KEY_PREFIX ?? 'store-scraper-bot:';
   log(`prefix: ${PREFIX}  (must match bot runtime KEY_PREFIX)`);
   // for each entry:
   await redis.set(`${PREFIX}${key}`, JSON.stringify(value), ttl ? { ex: ttl } : undefined);
   ```
3. Add `--dry-run` flag: print actions without writing to Upstash.
4. Drop the `entries.length > 10000` bulk-put cap (Redis has no such limit).
5. Update `package.json`:
   ```json
   "migrate": "node --env-file=.env.deploy scripts/migrate-atlas-to-upstash.js",
   "migrate:dry": "node --env-file=.env.deploy scripts/migrate-atlas-to-upstash.js --dry-run"
   ```
   `.env.deploy` carries both `MONGODB_URI` (Atlas read) and `UPSTASH_REDIS_REST_URL`/`_TOKEN` (Upstash write).
6. Delete `scripts/migrate-atlas-to-kv.js`.
7. Test: `npm run migrate:dry` — verify counts match expected admin (1) + groups (production count).

## Success Criteria

- [ ] `npm run migrate:dry` lists `admin` + all `group:*` keys with correct counts and prints effective `KEY_PREFIX`
- [ ] `npm run migrate` writes to Upstash; physical keys carry the prefix (e.g. `store-scraper-bot:admin`, `store-scraper-bot:group:42`)
- [ ] Admin + group counts in Upstash dashboard (filtered by prefix) match Atlas
- [ ] `admin` doc value byte-identical between Atlas and Upstash (read-back compare with prefix)
- [ ] Re-run is idempotent (no duplicate keys, same final state)
- [ ] `scripts/migrate-atlas-to-kv.js` deleted; `package.json` no longer references `migrate:bulk`

## Risk Assessment

- **Risk:** Atlas auto-pause (free tier) delays first connection ~5–30 s. **Mitigation:** bump `serverSelectionTimeoutMS` from 5000 to 30000 for migration tolerance. One-shot, not perf-critical.
- **Risk:** Migration runs while Java bot still writes to Atlas (lost writes during gap). **Mitigation:** Phase 6 cutover stops Java bot polling BEFORE migration; downtime <5 min.
- **Risk:** `@upstash/redis` SDK auto-stringifies values inconsistently. **Mitigation:** explicitly `JSON.stringify` values before `redis.set`; verify by reading back via `redis.get` and comparing.
- **Risk:** TTL on cache keys near `APP_CACHE_SECONDS` boundary lands with near-zero TTL. **Mitigation:** `KV_MIN_TTL_SECONDS = 60` clamp from existing script keeps Redis happy.
- **Risk:** Atlas data exceeds Upstash free 256 MB cap. **Mitigation:** bot has <100 keys; sub-megabyte. Even with cache, few MB max.
- **Risk:** `KEY_PREFIX` differs between migration script and Vercel env at runtime ⇒ bot reads from a different namespace and sees empty state. **Mitigation:** script logs prefix on start; Phase 6 success criteria includes verifying bot can `/listgroup` post-cutover (tests prefix alignment end-to-end).
