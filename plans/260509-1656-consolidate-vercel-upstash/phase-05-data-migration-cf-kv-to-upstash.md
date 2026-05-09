---
phase: 5
title: "Data migration CF KV → Upstash"
status: pending
priority: P1
effort: 30 min
dependencies: [2]
---

# Phase 5: Data migration CF KV → Upstash

## Overview

One-shot migration script: read all keys from Cloudflare KV via `wrangler kv key list` + `wrangler kv key get`, write them to Upstash Redis with TTL preserved on cache entries.

## Requirements

- Migrate `admin`, `group:*` keys (must preserve)
- Skip `apple:*` and `google:*` cache keys by default (auto-rebuild from upstream); add `--include-cache` flag for parity
- Idempotent — safe to re-run
- Preserves TTL on cache entries (compute remaining seconds)

## Architecture

Two-step, mirroring existing `migrate-atlas-to-kv.js` pattern:

```
scripts/migrate-cf-kv-to-upstash.js
  1. wrangler kv key list --binding STORE_KV --remote → JSON of key names
  2. for each key: wrangler kv key get → value + metadata.expiration
  3. write to Upstash via @upstash/redis SET with EX (if TTL applies)
  4. log counts: admin, group, apple, google
```

Use `wrangler` CLI (already devDep) via `child_process.execFileSync` rather than re-implementing CF API auth.

## Related Code Files

- Create: `scripts/migrate-cf-kv-to-upstash.js`
- Modify: `package.json` add script `migrate:upstash`

## Implementation Steps

1. Create `scripts/migrate-cf-kv-to-upstash.js`:
   - Reads `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN` from `.env.deploy`
   - Reads CF binding name from arg or default `STORE_KV`
   - Calls `wrangler kv key list --binding STORE_KV --remote` to enumerate
   - For each key: `wrangler kv key get --binding STORE_KV --remote <key>` (returns raw value)
   - Posts to Upstash via `Redis` SDK: `await client.set(key, value, ttl ? { ex: ttl } : {})`
   - `--include-cache` flag controls apple:/google: prefixes
   - `--dry-run` flag prints actions without writing
2. Add `package.json` script:
   ```json
   "migrate:upstash": "node --env-file=.env.deploy scripts/migrate-cf-kv-to-upstash.js"
   ```
3. Test in dry-run mode against current CF KV; verify key counts match expected admin + groups.

## Success Criteria

- [ ] Dry-run output lists all `admin` and `group:*` keys from CF KV
- [ ] Real run with --dry-run=false writes to Upstash
- [ ] Post-run: `redis-cli SCAN MATCH 'group:*'` (or Upstash REST equivalent) returns same group count as CF KV had
- [ ] `admin` key value matches byte-for-byte across CF and Upstash
- [ ] Re-run is idempotent (overwrites with same data)

## Risk Assessment

- **Risk:** `wrangler kv key list` doesn't expose TTL in default JSON output. **Mitigation:** `--include-cache` flag uses key metadata `expiration` if available; if not, skip TTL preservation and let cache rebuild (acceptable per design).
- **Risk:** Wrangler authenticates against the user's CF account; need `wrangler login` done before run. **Mitigation:** doc note in phase + check at script start.
- **Risk:** Large key count (>10k) blows out Wrangler shell pipeline. **Mitigation:** bot has <100 keys; not a concern. If grows, batch via cursor.
- **Risk:** Migration runs while bot still serving on CF (writes after migration are lost). **Mitigation:** cutover sequence in Phase 6 — migrate AFTER pausing CF webhook, BEFORE setting Vercel webhook.
