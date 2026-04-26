# Phase 03 — MongoDB Worker Adapter

## Context Links
- Existing: `src/repository/mongodb.js`
- miti99bot proven pattern: `/config/workspace/tiennm99/miti99bot/src/db/mongo-*.js`
- Atlas docs: https://www.mongodb.com/docs/drivers/node/current/

## Overview
- **Priority:** P0 — required by all 4 repository files.
- **Status:** pending
- **Description:** Adapt `src/repository/mongodb.js` for the Workers runtime: connection memoization per warm isolate, `MongoServerSelectionError` catch path, removal of `process.exit`-style shutdown. Repository files (`admin-repository.js`, etc.) remain **unchanged** because they consume `getCollection(name)`.

## Key Insights
- Workers isolates are short-lived but reused while warm; **memoize the `MongoClient` at module scope**, not per-request.
- Connecting on every request would cost ~1500ms cold-start + connection slot exhaustion (M0 cap = 500).
- M0 auto-pauses after 30 days idle. Driver throws `MongoServerSelectionError` after `serverSelectionTimeoutMS` (default 30s). **Lower this to 5000ms** to fail fast.
- The existing module-level `let client`/`let database` pattern works fine — just replace `await client.connect()` strategy with lazy + memoized.
- Workers don't fire process signals — `closeMongoDB()` is dead code on Workers; keep it as no-op for compatibility but remove the `process.on(...)` callers in Phase 04.

## Requirements

### Functional
- `getDatabase(env)` — lazy-init pattern, memoizes per isolate. Takes `env` (Workers-injected) instead of a global `config`.
- `getCollection(name, env)` — returns collection from memoized DB.
- Catches `MongoServerSelectionError` → re-throws a typed `MongoUnavailable` error consumable by command handlers (which return graceful "Internal server error" already).
- `serverSelectionTimeoutMS: 5000`, `socketTimeoutMS: 10000`.
- `appName: 'js-store-scraper-bot'` for Atlas observability.
- Repository files (`admin-repository.js`, `group-repository.js`, `apple-app-repository.js`, `google-app-repository.js`) updated minimally to thread `env` through the call chain. **OR** — a `getStore(env)` factory builds bound repos. Pick the second; it's cleaner.

### Non-functional
- No top-level `await` (Workers ESM doesn't allow during cold init in some runtimes).
- No global side effects on import.

## Architecture

```
src/repository/mongodb.js
  ├─ memoizedClient (module-scope, per isolate)
  ├─ getMongo(env) → memoized Promise<{ client, db }>
  └─ getCollection(name, env) → db.collection(name)

src/repository/store.js (NEW)
  └─ createStore(env) → {
       admin: { init, get, save, addGroup, removeGroup, hasGroup, getAllGroups },
       group: { exists, get, save, init, delete, addAppleApp, removeAppleApp, addGoogleApp, removeGoogleApp },
       appleApp: { get, save, getCached },
       googleApp: { get, save, getCached },
     }

src/bot/commands/*.js
  └─ accept `store` param instead of importing repos directly
```

## Related Code Files

### CREATE
- `src/repository/store.js` — factory binding `env` once, returning the four repos.

### MODIFY
- `src/repository/mongodb.js` — switch to memoized lazy init, `env`-driven, fast-fail timeouts, `MongoUnavailable` typed error.
- `src/repository/admin-repository.js` — convert from module-level functions to factory `createAdminRepository(env)` returning the same shape.
- `src/repository/group-repository.js` — same conversion.
- `src/repository/apple-app-repository.js` — same; `getCachedAppleApp(appId, appCacheSeconds, env)` accepts `env` (or comes from factory closure).
- `src/repository/google-app-repository.js` — same.

### DELETE
- (none)

## Implementation Steps

1. **Rewrite `src/repository/mongodb.js`**:
   ```js
   import { MongoClient } from 'mongodb';

   export class MongoUnavailable extends Error {
     constructor(cause) { super('MongoDB unavailable: ' + cause.message); this.cause = cause; }
   }

   let memoized = null;

   export async function getMongo(env) {
     if (memoized) return memoized;
     memoized = (async () => {
       try {
         const client = new MongoClient(env.MONGODB_URI, {
           serverSelectionTimeoutMS: 5000,
           socketTimeoutMS: 10000,
           appName: 'js-store-scraper-bot',
         });
         await client.connect();
         const db = client.db(); // db inferred from URI path
         return { client, db };
       } catch (err) {
         memoized = null; // allow retry on next request
         throw new MongoUnavailable(err);
       }
     })();
     return memoized;
   }

   export async function getCollection(name, env) {
     const { db } = await getMongo(env);
     return db.collection(name);
   }
   ```

2. **Write `src/repository/store.js`**:
   ```js
   import { createAdminRepository } from './admin-repository.js';
   import { createGroupRepository } from './group-repository.js';
   import { createAppleAppRepository } from './apple-app-repository.js';
   import { createGoogleAppRepository } from './google-app-repository.js';

   export function createStore(env) {
     return {
       admin: createAdminRepository(env),
       group: createGroupRepository(env),
       appleApp: createAppleAppRepository(env),
       googleApp: createGoogleAppRepository(env),
     };
   }
   ```

3. **Convert each repository to factory shape** (example for admin):
   ```js
   // src/repository/admin-repository.js
   import { getCollection } from './mongodb.js';
   import { ADMIN_ID, /* ... */ } from '../models/admin.js';

   export function createAdminRepository(env) {
     async function collection() { return getCollection('common', env); }

     async function getAdmin() {
       const c = await collection();
       const doc = await c.findOne({ _id: ADMIN_ID });
       return doc ?? newAdmin();
     }
     /* ...same logic, but every method awaits collection() ... */

     return { initAdmin, getAdmin, save, addGroup, removeGroup, hasGroup, getAllGroups };
   }
   ```
   Repeat for `group`, `appleApp`, `googleApp` repositories. Logic body unchanged; only top-level binding changes.

4. **Update each command in `src/bot/commands/*.js`** that imports repos:
   - Change `import * as adminRepo from '../../repository/admin-repository.js'` → command factories receive `store` param.
   - Example: `createAddGroupCommand(config, store)` instead of `createAddGroupCommand(config)`.
   - The `bot.js` from Phase 02 wires `store` into each command at construction time.

5. **Audit caller chain** — every command that touches a repo gets `store` from the bot factory. Total: 8 commands touch repos (everything except `info`, `rawappleapp`, `rawgoogleapp`, `check-app-scores` — wait, that one does too; recount during implementation).

6. **Sanity check**: `node --check src/repository/*.js`.

## Todo List
- [ ] `src/repository/mongodb.js` rewritten (memoized, env-driven, `MongoUnavailable`)
- [ ] `src/repository/store.js` written
- [ ] All 4 repository files converted to factory shape
- [ ] All command factories updated to accept `store`
- [ ] `src/bot/bot.js` wires `store` into commands
- [ ] `node --check src/**/*.js` passes
- [ ] Manual test in `wrangler dev`: `/info` and one DB-touching command (e.g. `/listgroup`) return correctly

## Success Criteria
- Connection is opened once per warm isolate (verify via Atlas dashboard "Current Connections" stays low).
- Cold start with one DB roundtrip stays under measured `BASELINE_COLD_PING_MS` from Phase 01.
- `MongoUnavailable` caught at command boundary → "Internal server error" message (existing behavior).

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Connection leaks across isolates | L | M | Memoize at module scope, never close in scheduled handler |
| `serverSelectionTimeoutMS: 5000` too aggressive | L | Low | Phase 01 measured baseline; 5s is 3× headroom |
| Repo factory refactor breaks commands | M | Medium | Each command's imports are explicit; sanity-check after each |

## Security Considerations
- `env.MONGODB_URI` only ever read inside `getMongo()`. Never logged. Lint enforces.

## Rollback
1. Restore `src/repository/*.js` from git.
2. Revert command-factory signatures in `src/bot/commands/*.js`.

## Next Steps
- **Blocks:** Phase 04 (entry handler injects `store` into `createBot`).
- **Unblocks:** Phase 04.
