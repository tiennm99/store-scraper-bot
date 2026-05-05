---
phase: 1
title: "Replace Mongo with KV in repositories"
status: completed
priority: P1
effort: "1.5h"
dependencies: []
---

# Phase 01: Replace Mongo with KV in repositories

## Overview

Swap the four `*-repository.js` modules from `mongodb` collection ops to `env.STORE_KV.get/put/delete`. Drop the `mongodb` driver dependency. Keep document shape and Java parity intact.

## Requirements

**Functional**
- All 13 Telegram commands continue to work without behavior change.
- Daily cron continues to enumerate groups and report stale apps.
- Cached app entries expire after `APP_CACHE_SECONDS`.

**Non-functional**
- Worker bundle ≤ 500 KiB.
- No `nodejs_compat_v2` flag required.
- `npm install` no longer pulls `mongodb`.

## Architecture

KV access pattern (single namespace `STORE_KV`, prefixed keys):

```
admin                       → JSON of {class:"Admin", groups:[...]}
group:{chatId}              → JSON of {class:"Group", _id, appleApps[], googleApps[]}
apple:{appId}               → JSON of cached Apple response (KV TTL = APP_CACHE_SECONDS)
google:{appId}              → JSON of cached Google response (KV TTL = APP_CACHE_SECONDS)
```

Read pattern: `await env.STORE_KV.get(key, 'json')` returns parsed JSON or `null`.
Write pattern: `await env.STORE_KV.put(key, JSON.stringify(value), opts?)`.
Delete pattern: `await env.STORE_KV.delete(key)`.

Cache TTL: pass `{ expirationTtl: appCacheSeconds }` to `put` for `apple:` and `google:` keys. **KV minimum `expirationTtl` is 60s** — Java/Mongo had no such floor. Guard in `kv.js`: clamp `expirationTtl` to `Math.max(60, value)` so an aggressive `APP_CACHE_SECONDS` override doesn't make `put` reject. Document the floor in `.dev.vars.example`.

Drop the `isAppleAppExpired` / `isGoogleAppExpired` helpers from the read path — KV deletes expired keys, so a `get` returning `null` is the cache miss. Keep the helpers if other code references them; otherwise delete.

## Related Code Files

**Replace (rewrite contents)**
- `src/repository/admin-repository.js` — KV ops on key `admin`
- `src/repository/group-repository.js` — KV ops on key `group:{chatId}`
- `src/repository/apple-app-repository.js` — KV ops on key `apple:{appId}` with TTL
- `src/repository/google-app-repository.js` — KV ops on key `google:{appId}` with TTL

**Delete**
- `src/repository/mongodb.js` — no longer needed

**Modify**
- `src/repository/store.js` — remove memoization comment about `MongoClient`; factory signature unchanged
- `src/index.js` — drop the "memoized MongoClient" comment block; nothing else changes (the `env` already flows through)
- `src/config.js` — remove `MONGODB_URI` from required env list
- `package.json` — `npm uninstall mongodb`; bump version to 0.3.0

**Untouched (verify)**
- `src/models/*.js` — document shape preserved verbatim (Java parity at the doc level: `_id`, `class`, all fields)
- `src/bot/commands/*.js` — repository interfaces unchanged
- `src/scheduler/scheduler.js` — calls `store.admin.getAllGroups()` and `store.group.getGroup()`; both still resolve

## Java parity preserved (per existing parity reports)

Cross-checked against `plans/reports/xia-260429-1601-store-scraper-bot-port-status.md` and `researcher-260429-1555-java-vs-js-parity.md`. KV swap preserves every PARITY item:

- Schema (`_id`, `class`, field names) — written verbatim as JSON values.
- Cache TTL semantics — KV `expirationTtl` is behaviorally equivalent to Java's `(now - millis) > cacheMillis` check on read. Both produce a "cache miss → re-fetch" outcome at the same boundary.
- `getAdmin()` returning `newAdmin()` if doc missing — `getJson('admin') ?? newAdmin()` matches.
- Existence checks — `(await get) !== null` replaces `countDocuments`.
- Concurrent `addGroup` race — Java/Mongo had the same non-atomic R-M-W; not a regression.

**Two intentional divergences from Java:**

1. **Eventual consistency (~60s globally)**: Java/Mongo gave strong read-after-write. For a single-admin daily-cron bot this is acceptable; documented in `plan.md`.
2. **TTL floor of 60s**: Java had no minimum. Mitigated by the `kv.js` clamp above.

## Implementation Steps

1. **Write `kv.js` helper** at `src/repository/kv.js` — thin wrapper exposing `getJson(env, key)`, `putJson(env, key, value, opts?)`, `del(env, key)`. Single-binding-name constant `STORE_KV`. Throws a typed error if `env.STORE_KV` is missing.

2. **Rewrite `admin-repository.js`**:
   - `init()` → `getJson('admin')`; if null, `putJson('admin', newAdmin())`.
   - `getAdmin()` → `getJson('admin') ?? newAdmin()`.
   - `save(admin)` → `putJson('admin', admin)`.
   - `addGroup`/`removeGroup`/`hasGroup`/`getAllGroups` — unchanged (delegate to `getAdmin` + `save`).

3. **Rewrite `group-repository.js`**:
   - `key(groupId)` → `` `group:${groupIdToKey(groupId)}` ``.
   - `exists(groupId)` → `getJson(key(groupId)) !== null`.
   - `getGroup(groupId)` → `getJson(key(groupId)) ?? newGroup(groupId)`.
   - `saveGroup(group)` → `putJson(\`group:${group._id}\`, group)`.
   - `deleteGroup(groupId)` → `del(key(groupId))`.
   - `mutateAndSave` unchanged.

4. **Rewrite `apple-app-repository.js`**:
   - `key(appId)` → `` `apple:${appId}` ``.
   - `get(appId)` → `getJson(key(appId))`.
   - `save(entry)` → `putJson(key(entry._id), entry, { expirationTtl: appCacheSeconds })`.
   - `getCached(appId)` → just `get(appId)` — TTL already enforced by KV. Remove the `isAppleAppExpired` call.

5. **Rewrite `google-app-repository.js`** — same pattern as apple.

6. **Delete `src/repository/mongodb.js`**. Remove its imports from the four repositories.

7. **Update `src/config.js`** — drop `'MONGODB_URI'` from the `required` list.

8. **Update `src/index.js`** — strip the `MongoClient`-related comment in `build()`. Add a sanity check: throw if `env.STORE_KV` is undefined (early failure beats null-dereference at first request).

9. **Update `package.json`**:
   - `npm uninstall mongodb`
   - bump `version` to `0.3.0`

10. **Compile/lint check**: `node --check` each modified file. Run `npm run lint` (the existing secret-leak check).

11. **Local sanity (no live KV needed)**: `node -e "import('./src/repository/store.js').then(...)"` — confirm no import errors. Cannot fully test without `wrangler dev` (Phase 03).

## Todo List

- [ ] Create `src/repository/kv.js` helper
- [ ] Rewrite `admin-repository.js`
- [ ] Rewrite `group-repository.js`
- [ ] Rewrite `apple-app-repository.js` (with KV TTL)
- [ ] Rewrite `google-app-repository.js` (with KV TTL)
- [ ] Delete `src/repository/mongodb.js`
- [ ] Update `src/config.js` required env list
- [ ] Update `src/index.js` env validation
- [ ] `npm uninstall mongodb`; bump `package.json` version
- [ ] `node --check` all modified files
- [ ] `npm run lint`
- [ ] **Java parity check**: spot-diff a written `group:{id}` JSON value against an equivalent Java/Mongo doc structure (sample from upstream `tiennm99/store-scraper-bot` model classes). Field names, ordering-insensitive, must match.

## Success Criteria

- [ ] `grep -rn "mongodb\|MongoClient" src/` returns nothing.
- [ ] `package.json` `dependencies` is empty (only `wrangler` in devDeps).
- [ ] All 4 repository factories return the same interface they did before (consumers untouched).
- [ ] `node --check src/index.js` exits 0.
- [ ] `npm run lint` passes.

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Repository interface drift breaks command handlers | Low | Keep the exported function names + signatures byte-identical |
| KV's eventual consistency surprises a user mid-conversation | Low | Single-admin bot, low concurrency; document in README |
| Concurrent `/addgroup` race clobbers `groups[]` | Low | Same race existed in Mongo; not a regression |
| TTL `expirationTtl` < 60 silently fails | Low | `APP_CACHE_SECONDS` defaults to 600; doc the floor in `.dev.vars.example` |

## Security Considerations

- KV namespace ID is not a secret (binding name is what code references). Safe to commit in `wrangler.toml`.
- Removing `MONGODB_URI` removes one secret from the rotation surface. Update `wrangler secret list` documentation in Phase 05.
