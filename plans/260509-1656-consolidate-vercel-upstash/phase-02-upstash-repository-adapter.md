---
phase: 2
title: "Upstash repository adapter"
status: pending
priority: P1
effort: 1h
dependencies: [1]
---

# Phase 2: Upstash repository adapter

## Overview

Replace `src/repository/kv.js` Cloudflare KV wrapper with an Upstash Redis equivalent. Repository files (`admin-repository.js`, `group-repository.js`, `apple-app-repository.js`, `google-app-repository.js`) keep their public APIs; only the storage primitive changes.

## Requirements

- Function-level API of `kv.js` preserved (`getJson`, `putJson`, `del`)
- TTL semantics preserved (Apple/Google cache uses `expirationTtl` → Redis `EX`)
- Drops `env.STORE_KV` binding; takes Upstash client instance instead
- 60s minimum TTL clamp removed (Redis `EX` accepts 1s+, but keep clamp for parity safety)
- **Multi-tenancy isolation:** every key gets a namespace prefix (`KEY_PREFIX` env var, default `store-scraper-bot:`) so this bot's data cannot collide with other Vercel projects sharing the same Upstash DB. Applied transparently in the adapter — repository code stays prefix-unaware.

## Architecture

```
src/repository/
├── upstash.js              ← NEW: client factory + getJson/putJson/del/scan
├── kv.js                   ← DELETE (or rename to keep history)
├── store.js                ← takes upstash client instead of env
├── admin-repository.js     ← unchanged signatures, internal: env→client
├── group-repository.js     ← unchanged signatures
├── apple-app-repository.js ← unchanged signatures
└── google-app-repository.js← unchanged signatures
```

Client construction with namespace prefix:
```js
import { Redis } from '@upstash/redis';

export function createUpstashClient(env) {
  const client = new Redis({
    url: env.UPSTASH_REDIS_REST_URL,
    token: env.UPSTASH_REDIS_REST_TOKEN,
  });
  const prefix = env.KEY_PREFIX ?? 'store-scraper-bot:';
  return { client, prefix };
}

const k = (prefix, key) => `${prefix}${key}`;
```

All adapter functions (`getJson`, `putJson`, `del`, `scan`) prepend `prefix` before hitting Redis. Repository callers pass logical keys (`admin`, `group:42`, etc.); the adapter translates to physical keys (`store-scraper-bot:admin`, `store-scraper-bot:group:42`, etc.). Same logic applies to scan match patterns.

API mapping:
| CF KV | Upstash Redis | Note |
|---|---|---|
| `kv.get(key, 'json')` | `redis.get(prefix+key)` | Upstash auto-deserializes JSON when value was `JSON.stringify`d on `set` |
| `kv.put(key, value)` | `redis.set(prefix+key, value)` | |
| `kv.put(key, value, { expirationTtl: N })` | `redis.set(prefix+key, value, { ex: N })` | Redis `EX` is seconds |
| `kv.delete(key)` | `redis.del(prefix+key)` | |
| `kv.list({ prefix: 'group:' })` | `redis.scan(0, { match: prefix+'group:*' })` | Phase 5 migration uses this |

## Related Code Files

- Create: `src/repository/upstash.js`
- Modify: `src/repository/store.js` (accept client, not env)
- Modify: `src/repository/admin-repository.js` (use client)
- Modify: `src/repository/group-repository.js` (use client)
- Modify: `src/repository/apple-app-repository.js` (use client)
- Modify: `src/repository/google-app-repository.js` (use client)
- Delete: `src/repository/kv.js`

## Implementation Steps

1. Write `src/repository/upstash.js`:
   - `createUpstashClient(env)` returns `{ client, prefix }` object (prefix from `env.KEY_PREFIX ?? 'store-scraper-bot:'`)
   - `getJson(handle, key)`, `putJson(handle, key, value, { expirationTtl } = {})`, `del(handle, key)`, `scan(handle, matchSuffix)` all prepend `handle.prefix` to the key before calling `handle.client`
   - Mirror `kv.js` signatures so repository files only swap import path + first arg
2. Update `store.js` to accept the handle instead of `env`:
   ```js
   export function createStore(handle, appCacheSeconds) { ... }
   ```
3. Update each repository to import from `./upstash.js` and take `handle` instead of `env`. Search/replace `env, key` → `handle, key` in each file. Repositories continue passing logical keys (`admin`, `group:${id}`, etc.) — they never see the prefix.
4. Delete `src/repository/kv.js`.
5. Manual smoke: `node -e "import('./src/repository/upstash.js').then(m => console.log(m))"` to confirm import path resolves.
6. Verify isolation: with `KEY_PREFIX=store-scraper-bot:`, write a test value via `putJson(handle, 'admin', {x:1})` and confirm in Upstash dashboard that the physical key is `store-scraper-bot:admin`.

## Success Criteria

- [ ] `src/repository/upstash.js` exports `createUpstashClient`, `getJson`, `putJson`, `del`, `scan`
- [ ] `createUpstashClient` reads `KEY_PREFIX` (default `store-scraper-bot:`) and bundles it into the returned handle
- [ ] All four `*-repository.js` files import from `./upstash.js`, take `handle`
- [ ] `store.js` signature: `createStore(handle, appCacheSeconds)`
- [ ] `kv.js` deleted
- [ ] No `env.STORE_KV` references remain in `src/`
- [ ] Smoke write produces a physically-prefixed key in Upstash (e.g., `store-scraper-bot:admin`)

## Risk Assessment

- **Risk:** `@upstash/redis` SDK auto-stringifies/parses values inconsistently. **Mitigation:** read SDK docs; if auto-JSON disabled, mirror `kv.js`'s `JSON.stringify`/`JSON.parse` explicitly.
- **Risk:** Redis `SET key value EX 0` is invalid; current `KV_MIN_TTL_SECONDS = 60` clamp keeps us safe. Keep the clamp.
- **Risk:** Redis returns `null` on missing key (same as KV). Verify in unit smoke.
- **Risk:** `KEY_PREFIX` mismatch between bot runtime and migration script ⇒ migrated data unreadable. **Mitigation:** both reads from same `.env.deploy` / Vercel env var; document the contract in README. Default value `store-scraper-bot:` is the single source of truth — only override if explicitly multi-tenant.
- **Risk:** Forgetting to apply prefix to `scan` match patterns causes data leak / misses. **Mitigation:** centralize all key composition inside `upstash.js`; repositories must never pass raw match patterns to the client.
