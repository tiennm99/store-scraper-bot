---
phase: 6
title: "One-shot Atlas → KV data migration"
status: pending
priority: P1
effort: "1h"
dependencies: [2]
---

# Phase 06: One-shot Atlas → KV data migration

## Overview

Operator runs a local Node script that exports `common` and `group` collections from MongoDB Atlas and bulk-imports them into the production KV namespace. Cache collections are skipped by default (they auto-rebuild from upstream APIs).

**Run order**: must execute **after Phase 02** (KV namespace provisioned) and **before Phase 04** (smoke tests need real data). Phase 03 deploy can happen before or after — the script writes via `wrangler kv bulk put`, not via the deployed Worker.

## Requirements

**Functional**
- Every `_id="admin"` doc in `common` lands at KV key `admin`.
- Every doc in `group` lands at KV key `group:{_id}`.
- With `--include-cache`: `apple_app` → `apple:{_id}` and `google_app` → `google:{_id}`, each with `expirationTtl` recomputed from the doc's `millis` field. Already-expired entries are skipped.
- Re-running the script overwrites — no duplication, no partial-state corruption.

**Non-functional**
- Script is one file under `scripts/`. ≤100 LOC.
- `mongodb` driver lives in `devDependencies` only; not pulled into the Worker bundle.
- Sensitive intermediate file (`scripts/.atlas-export.json`) is deleted after success.

## Architecture

```
.env (operator-local)        Atlas (read-only)
   |                              |
   v                              v
[migrate-atlas-to-kv.js] ─── reads all docs from `common`+`group`(+caches?)
                              |
                              v
                  builds bulk JSON [{key, value, expiration_ttl?}, ...]
                              |
                              v
              wrangler kv bulk put --binding STORE_KV --remote
                              |
                              v
                       Cloudflare KV
```

Key shapes mirror what the runtime code expects (see `src/repository/*-repository.js`):

| Atlas collection | KV key | TTL |
|---|---|---|
| `common` | `admin` (singleton, _id="admin") | none |
| `group` | `group:{_id}` | none |
| `apple_app` | `apple:{_id}` | `max(60, (millis + APP_CACHE_SECONDS*1000 - now) / 1000)`, skip if ≤0 |
| `google_app` | `google:{_id}` | same |

`wrangler kv bulk put` accepts up to 10,000 keys per call. For this bot, total keys are likely <100 (one admin + a handful of groups), so bulk fits in a single request.

## Related Code Files

**Create**
- `scripts/migrate-atlas-to-kv.js` — the migration script.

**Modify**
- `package.json` — add `mongodb` to `devDependencies`; add `migrate` npm script.
- `README.md` — short "Migrating from Atlas (one-time)" section.

**Untouched**
- All `src/` repositories — they read from KV; the script writes to KV via wrangler. No code change needed in the Worker.

## Implementation Steps

1. **Add `mongodb` as devDep**: `npm install --save-dev mongodb`.

2. **Write `scripts/migrate-atlas-to-kv.js`**:
   - `import { MongoClient } from 'mongodb'`.
   - Read `MONGODB_URI` from `process.env` (loaded via `node --env-file=.env`).
   - Connect with 5s timeout; pick db from URI path.
   - `const adminDocs = await db.collection('common').find({ _id: 'admin' }).toArray()`.
   - `const groupDocs = await db.collection('group').find({}).toArray()`.
   - If `--include-cache` flag: read `apple_app` and `google_app` similarly; compute `expiration_ttl` per doc, skip expired.
   - Build entries: `{ key, value: JSON.stringify(doc), expiration_ttl?: number }`.
   - Write to `scripts/.atlas-export.json`.
   - `console.log` summary: `{admin: 1, groups: N, apple: M (skipped K), google: ...}`.
   - Print the next-step command: `npx wrangler kv bulk put --binding STORE_KV --remote scripts/.atlas-export.json`.

3. **Add npm scripts to `package.json`**:
   ```json
   "migrate": "node --env-file=.env scripts/migrate-atlas-to-kv.js",
   "migrate:bulk": "wrangler kv bulk put --binding STORE_KV --remote scripts/.atlas-export.json"
   ```

4. **`.gitignore`** — add `scripts/.atlas-export.json` (already covers `.env` patterns; confirm `.atlas-export.json` is ignored).

5. **README "Migrating from Atlas" section**:
   ```
   1. Ensure .env has MONGODB_URI pointing to your Atlas cluster.
   2. npm install (pulls mongodb devDep).
   3. npm run migrate           # writes scripts/.atlas-export.json
   4. npm run migrate:bulk      # uploads to KV
   5. rm scripts/.atlas-export.json   # contains your data; delete after success
   6. Optional: npm run migrate -- --include-cache  to also migrate cached app entries
   ```

6. **Verify** post-import:
   ```sh
   npx wrangler kv key list --binding STORE_KV --remote | jq 'length'
   npx wrangler kv key get --binding STORE_KV --remote admin | jq
   ```
   Expect at least 1 (admin) + N (groups) keys.

## Todo List

- [ ] `npm install --save-dev mongodb`
- [ ] Write `scripts/migrate-atlas-to-kv.js`
- [ ] Add `migrate` + `migrate:bulk` scripts to `package.json`
- [ ] Add `scripts/.atlas-export.json` to `.gitignore`
- [ ] Add "Migrating from Atlas" section to README
- [ ] Operator runs end-to-end against staging KV namespace first; verify with `kv key list`
- [ ] Operator runs against production KV namespace
- [ ] Delete `scripts/.atlas-export.json`

## Success Criteria

- [ ] `wrangler kv key get --remote admin` returns the admin doc with `groups` array intact.
- [ ] Each group has its `apple:{appId}` / `google:{appId}` references resolvable through the bot's `/listapp` command (verified in Phase 04 smoke).
- [ ] Re-running `npm run migrate && npm run migrate:bulk` produces zero diff in KV (idempotent).
- [ ] `scripts/.atlas-export.json` is gitignored and deleted after success.

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Atlas auth fails | Low | Pre-flight: verify URI works via `mongosh "$MONGODB_URI"` before running migrate |
| Bulk put exceeds 10k keys (unlikely for a handful of groups) | Very Low | Script can chunk into multiple files if `entries.length > 10000` (skip implementing — defer until needed) |
| Operator runs against wrong namespace | Medium | The `--remote` flag targets prod; review the `wrangler.toml` binding ID before running |
| Sensitive data leaks via committed bulk JSON | Low | `.gitignore` entry; README warns to delete |
| Cache TTL miscompute (e.g. negative ttl) | Low | Skip rule: `if (newTtl < 60) skip`. Documented in step 2 |

## Security Considerations

- `scripts/.atlas-export.json` contains all bot state (group memberships, admin IDs). Treat as sensitive.
- Run `npm run lint` before committing to catch accidental Atlas URI in any new file.
- Consider rotating Atlas password after migration (its only legitimate consumer is gone).
