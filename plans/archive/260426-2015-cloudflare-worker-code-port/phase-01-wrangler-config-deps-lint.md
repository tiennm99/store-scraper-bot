# Phase 01 — Wrangler Config + Deps Swap + Lint Scaffolding

## Context Links
- miti99bot wrangler.toml: `/config/workspace/tiennm99/miti99bot/wrangler.toml`
- miti99bot secret-leak lint: `/config/workspace/tiennm99/miti99bot/scripts/check-secret-leaks.js`
- Existing `package.json`: `/config/workspace/tiennm99/js-store-scraper-bot/package.json`

## Overview
- **Priority:** P0
- **Status:** pending
- **Description:** Pure config + dependency changes. No application code touched. After this, `npm install` succeeds with the new dep set; `wrangler.toml` is in place; lint runs.

## Key Insights
- Workers cron is **UTC**: `0 0 * * *` UTC = 7am Asia/Ho_Chi_Minh.
- `nodejs_compat_v2` is the lever that gives `node:net`/`node:tls` (needed by `mongodb` driver).
- Secret-leak lint added now so subsequent phases can't sneak in a `console.log(env.MONGODB_URI)`.
- Removing Node-only deps in this phase causes `src/index.js` to fail to load — that's expected and fine; Phase 04 rewrites the entry. Until then, `npm start` is broken (acceptable; we're refactoring).

## Requirements

### Functional
- `wrangler.toml` written with `nodejs_compat_v2`, cron `0 0 * * *`, observability block.
- `package.json`:
  - Add: `mongodb@^6.7.0`
  - Remove: `node-telegram-bot-api`, `node-cron`, `dotenv`, `pino`, `pino-pretty`
  - Dev-add: `wrangler@^3`
  - Scripts: `dev`, `deploy`, `register`, `register:dry`, `lint`
  - `engines.node` stays `>=20`
  - Drop `type: module` is **not** needed — Workers ESM is the same syntax.
- `scripts/check-secret-leaks.js` written.
- `.dev.vars.example` written.
- `.env.deploy.example` written.
- `.gitignore` adds `.dev.vars`, `.env.deploy`, `.tmp-deploy/`, `.wrangler/`.

### Non-functional
- `npm install` succeeds.
- `npm run lint` succeeds (only secret-leak check at this point; will gain `node --check` in later phases).

## Architecture

```
js-store-scraper-bot/
├── wrangler.toml                       (NEW)
├── package.json                         (MODIFIED — deps swap)
├── .gitignore                           (MODIFIED — Worker artifacts)
├── .dev.vars.example                    (NEW)
├── .env.deploy.example                  (NEW)
├── scripts/
│   └── check-secret-leaks.js           (NEW)
└── src/                                  (UNTOUCHED in this phase)
```

## Related Code Files

### CREATE
- `wrangler.toml`
- `scripts/check-secret-leaks.js`
- `.dev.vars.example`
- `.env.deploy.example`

### MODIFY
- `package.json` — full rewrite of `dependencies`, `devDependencies`, `scripts`.
- `.gitignore` — append Worker artifact patterns.

### DELETE
- (none in this phase)

## Implementation Steps

1. **Write `wrangler.toml`**:
   ```toml
   name = "js-store-scraper-bot"
   main = "src/index.js"
   compatibility_date = "2025-10-01"
   compatibility_flags = ["nodejs_compat_v2"]

   [vars]
   APP_CACHE_SECONDS = "600"
   NUM_DAYS_WARNING_NOT_UPDATED = "30"

   # 0 UTC = 7am Asia/Ho_Chi_Minh
   [triggers]
   crons = ["0 0 * * *"]

   [observability]
   enabled = true
   head_sampling_rate = 1

   [observability.logs]
   enabled = true
   invocation_logs = true

   # Secrets (set via `wrangler secret put`, NOT here):
   #   TELEGRAM_BOT_TOKEN
   #   TELEGRAM_BOT_USERNAME
   #   TELEGRAM_WEBHOOK_SECRET
   #   MONGODB_URI
   #   ADMIN_IDS
   ```

2. **Rewrite `package.json`**:
   ```json
   {
     "name": "js-store-scraper-bot",
     "version": "0.2.0",
     "description": "JavaScript port of store-scraper-bot, deployable to Cloudflare Workers.",
     "type": "module",
     "private": true,
     "engines": { "node": ">=20" },
     "main": "src/index.js",
     "scripts": {
       "dev": "wrangler dev",
       "deploy": "wrangler deploy && npm run register",
       "register": "node --env-file=.env.deploy scripts/register-webhook.js",
       "register:dry": "node --env-file=.env.deploy scripts/register-webhook.js --dry-run",
       "lint": "node scripts/check-secret-leaks.js"
     },
     "dependencies": { "mongodb": "^6.7.0" },
     "devDependencies": { "wrangler": "^3" },
     "license": "Apache-2.0"
   }
   ```

3. **Write `scripts/check-secret-leaks.js`**:
   ```js
   #!/usr/bin/env node
   // Fails CI if any source file logs a secret.
   import { readdirSync, readFileSync, statSync } from 'node:fs';
   import { join } from 'node:path';

   const SECRETS = ['MONGODB_URI', 'TELEGRAM_BOT_TOKEN', 'TELEGRAM_WEBHOOK_SECRET', 'ADMIN_IDS'];
   const ROOTS = ['src', 'scripts'];

   function* walk(dir) {
     for (const entry of readdirSync(dir)) {
       const p = join(dir, entry);
       const s = statSync(p);
       if (s.isDirectory()) yield* walk(p);
       else if (/\.(js|mjs|ts)$/.test(p)) yield p;
     }
   }

   const violations = [];
   for (const root of ROOTS) {
     try {
       for (const file of walk(root)) {
         const text = readFileSync(file, 'utf8');
         for (const sec of SECRETS) {
           const re = new RegExp(`console\\.(log|info|warn|error|debug)\\([^)]*\\benv\\.${sec}\\b`);
           if (re.test(text)) violations.push({ file, secret: sec });
         }
       }
     } catch { /* ignore missing root */ }
   }

   if (violations.length) {
     console.error('Secret-leak violations:');
     for (const v of violations) console.error(`  ${v.file}: env.${v.secret} in console.*`);
     process.exit(1);
   }
   console.log('check-secret-leaks: clean');
   ```

4. **Write `.dev.vars.example`**:
   ```
   TELEGRAM_BOT_TOKEN=
   TELEGRAM_BOT_USERNAME=
   TELEGRAM_WEBHOOK_SECRET=
   MONGODB_URI=
   ADMIN_IDS=
   ```

5. **Write `.env.deploy.example`**:
   ```
   TELEGRAM_BOT_TOKEN=
   TELEGRAM_WEBHOOK_SECRET=
   WORKER_URL=https://js-store-scraper-bot.<account>.workers.dev
   ```

6. **Append to `.gitignore`**:
   ```
   .dev.vars
   .env.deploy
   .tmp-deploy/
   .wrangler/
   ```

7. **Reinstall**:
   ```sh
   rm -rf node_modules package-lock.json
   npm install
   ```

8. **Smoke**: `npm run lint` — must print `check-secret-leaks: clean`.

## Todo List
- [ ] `wrangler.toml` written
- [ ] `package.json` updated (deps swap + scripts)
- [ ] `scripts/check-secret-leaks.js` written
- [ ] `.dev.vars.example` written
- [ ] `.env.deploy.example` written
- [ ] `.gitignore` updated
- [ ] `npm install` succeeds with new deps
- [ ] `npm run lint` passes

## Success Criteria
- All 4 new files exist; `package.json` reflects new dep set; `npm install` clean.
- Lint script runs even though `src/` is unchanged (no false positives).

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `mongodb` install fails on this Node version | L | Medium | Node 20+ supported by mongodb 6.7 |
| `wrangler` install pulls huge tree | L | Low | Dev-only; not bundled |
| `src/index.js` no longer runs after deps removed | H | None | Expected — Phase 04 rewrites entry |

## Next Steps
- **Blocks:** Phase 02 (telegram-api.js relies on Worker-style fetch; doesn't depend on this phase's files but conceptually starts here), Phase 03.
- **Unblocks:** Phase 02, Phase 03 (parallel-safe).
