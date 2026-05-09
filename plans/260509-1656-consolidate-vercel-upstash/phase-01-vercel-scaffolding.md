---
phase: 1
title: "Vercel scaffolding + deps"
status: pending
priority: P1
effort: 30 min
dependencies: []
---

# Phase 1: Vercel scaffolding + deps

## Overview

Add Vercel project config, install new runtime deps, restructure `package.json` for Node-on-Vercel. No behavior changes yet — just scaffolding.

## Requirements

- Vercel project recognizes the repo (Node 20 runtime, ESM)
- Daily cron registered (00:00 UTC = 07:00 Asia/Saigon)
- New deps installed; `mongodb` devDep stays (still needed by Phase 5 migration script)

## Architecture

```
vercel.json           cron schedule, function config (maxDuration, region)
package.json          + app-store-scraper, google-play-scraper,
                      + @upstash/redis, @vercel/functions
                      scripts: dev → vercel dev, deploy → vercel deploy
.vercelignore         exclude /plans, /scripts/.atlas-export.json
```

Vercel auto-detects `api/*.js` as serverless functions in Phase 4. This phase only sets the scaffolding.

## Related Code Files

- Create: `vercel.json`
- Create: `.vercelignore`
- Modify: `package.json` (deps, scripts)

## Implementation Steps

1. Add deps:
   ```sh
   npm install app-store-scraper@^0.18.0 google-play-scraper@^10.1.2 @upstash/redis @vercel/functions
   ```
2. Update `package.json` scripts:
   - `dev`: `vercel dev` (was `wrangler dev`)
   - `deploy`: `vercel deploy --prod && npm run register`
   - Keep `migrate`, `lint`. Drop `migrate:bulk` (CF-specific).
   - Add `register`: `node --env-file=.env.deploy scripts/register-webhook.js`
3. Create `vercel.json`:
   ```json
   {
     "crons": [{ "path": "/api/cron", "schedule": "0 0 * * *" }],
     "functions": {
       "api/cron.js": { "maxDuration": 60 },
       "api/webhook.js": { "maxDuration": 30 }
     }
   }
   ```
4. Create `.vercelignore` excluding `plans/`, `scripts/.atlas-export.json`, `*.md`, `Dockerfile`, `docker-compose*.yml`, `wrangler.toml` (until Phase 7 deletes it).
5. `npm install` and verify `node_modules` has all four new deps.

## Success Criteria

- [ ] `package.json` shows new deps + new scripts
- [ ] `vercel.json` parses (`vercel dev --debug` doesn't error on config)
- [ ] `.vercelignore` excludes irrelevant paths
- [ ] `npm install` exits clean

## Risk Assessment

- **Risk:** Vercel function bundle exceeds 250 MB with all deps. **Mitigation:** check `vercel build --debug` output; tree-shaking + `excludeFiles` pattern in `vercel.json` if needed. Expected size ~30 MB.
- **Risk:** `app-store-scraper` uses deprecated `request` lib that may warn or fail under Node 20. **Mitigation:** verify in Phase 3 by importing + calling `app()`. Fallback: pin Node to 18 or swap lib (out of scope).
