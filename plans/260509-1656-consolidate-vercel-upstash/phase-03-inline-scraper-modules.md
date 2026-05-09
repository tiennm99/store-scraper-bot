---
phase: 3
title: "Inline scraper modules"
status: pending
priority: P1
effort: 45 min
dependencies: [1]
---

# Phase 3: Inline scraper modules

## Overview

Replace `fetch('https://store-scraper.vercel.app/...')` with direct calls to `app-store-scraper` and `google-play-scraper` npm libs. Eliminates the cross-service HTTP roundtrip.

## Requirements

- `apple-scraper.js` and `google-scraper.js` keep their exported function signatures (`createAppleScraper`, `createGoogleScraper`, `buildAppleRequestBy*`, `buildGoogleRequest`) so command handlers don't change
- Behavior parity: `rawApp(req)` returns JSON string; `app(req)` returns parsed object
- Library `app()` method only — no other methods used today

## Architecture

```
src/api/apple-scraper.js
  ├─ removed: BASE_URL, fetch()
  └─ uses: import store from 'app-store-scraper'; store.app(params)

src/api/google-scraper.js
  ├─ removed: BASE_URL, fetch()
  └─ uses: import gplay from 'google-play-scraper'; gplay.app(params)
```

`rawApp` returns `JSON.stringify(result)` since libs return JS objects, not strings. Command `/rawappleapp` and `/rawgoogleapp` consumers expect text.

## Related Code Files

- Modify: `src/api/apple-scraper.js`
- Modify: `src/api/google-scraper.js`

## Implementation Steps

1. Edit `src/api/apple-scraper.js`:
   ```js
   import store from 'app-store-scraper';
   import { newAppleApp } from '../models/apple-app.js';

   export function buildAppleRequestByTrackId(id, country) {
     return { id, country, ratings: true };
   }
   export function buildAppleRequestByBundleId(appId, country) {
     return { appId, country, ratings: true };
   }

   export function createAppleScraper(config, storeRepo) {
     const { logger } = config;
     const repo = storeRepo.appleApp;

     async function app(req) {
       return await store.app(req);
     }
     async function rawApp(req) {
       return JSON.stringify(await app(req));
     }
     // cache, getApp, fetchAndCache: unchanged
     ...
   }
   ```
2. Edit `src/api/google-scraper.js` analogously, importing `gplay` from `google-play-scraper`.
3. Verify no other files import `BASE_URL` (grep `src/ -r 'BASE_URL'`).
4. Smoke from a Node REPL after `npm install` (Phase 1 added the deps):
   ```sh
   node -e "import('app-store-scraper').then(m => m.default.app({ appId: 'com.apple.weather', country: 'us' })).then(r => console.log(r.appId, r.version))"
   node -e "import('google-play-scraper').then(m => m.default.app({ appId: 'com.spotify.music', country: 'us' })).then(r => console.log(r.appId, r.version))"
   ```

## Success Criteria

- [ ] `grep -r 'store-scraper.vercel.app' src/` returns zero hits
- [ ] Local smoke calls succeed against both stores
- [ ] `apple-scraper.js`, `google-scraper.js` keep exported names; no other file modified
- [ ] Returned shape from `app()` matches what cache models (`newAppleApp`, `newGoogleApp`) expect

## Risk Assessment

- **Risk:** `app-store-scraper.app({ ratings: true })` makes 2 HTTPS calls (lookup + ratings.svg). Latency higher than single-fetch. **Mitigation:** acceptable for command-driven calls; cache TTL covers daily cron amplification.
- **Risk:** `google-play-scraper.app()` HTML scrape can fail when Google changes Play page markup. **Mitigation:** error already bubbles to user via existing try/catch in commands. Monitor first week post-deploy.
- **Risk:** `app-store-scraper` uses deprecated `request` lib — Node 20 warns but still runs. **Mitigation:** suppress with `NODE_NO_WARNINGS=1` if noisy. Long-term: track lib for replacement.
- **Risk:** Returned objects may have non-JSON-serializable fields (Dates). **Mitigation:** `JSON.stringify` produces ISO strings; consumers already parse strings.
