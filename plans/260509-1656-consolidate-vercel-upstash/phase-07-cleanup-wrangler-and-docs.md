---
phase: 7
title: "Cleanup wrangler + docker + docs"
status: completed
priority: P2
effort: 30 min
dependencies: [6]
---

# Phase 7: Cleanup wrangler + docker + docs

## Overview

After Vercel deploy is stable (≥7 days post-cutover), remove Cloudflare Workers + Docker artifacts (both irrelevant to Vercel deploy), retire the old `tiennm99/store-scraper` Vercel wrapper, and update README + docs to reflect the new architecture.

## Requirements

- Repo no longer references CF Workers, KV, `wrangler`, Docker, or local Mongo (except in `plans/` archive history)
- README accurately describes Vercel + Upstash setup
- One-shot migration script `migrate-atlas-to-upstash.js` removed (role done after Phase 6)
- `mongodb` devDep removed (Atlas migration script deleted with it)
- All three Docker files deleted: `Dockerfile`, `docker-compose.yml`, `docker-compose.dev.yml`
- `tiennm99/store-scraper` repo marked deprecated

## Architecture

Files to remove:

```
DELETE
├── wrangler.toml
├── Dockerfile                     (Mongo-based local container; obsolete with Upstash)
├── docker-compose.yml             (bot + mongo service; obsolete)
├── docker-compose.dev.yml         (dev mongo service; obsolete)
├── scripts/migrate-atlas-to-upstash.js  (one-shot done in Phase 6)
└── (CF Worker on dashboard) wrangler delete  (only if CF Worker was ever deployed)

MODIFY
├── package.json                   remove wrangler, mongodb devDeps; drop migrate scripts
├── README.md                      Vercel + Upstash setup; drop Atlas/Docker/KV history
└── docs/                          if any architecture docs reference CF or Docker
```

Phase 5 already deleted `scripts/migrate-atlas-to-kv.js`. Phase 7 removes the new `migrate-atlas-to-upstash.js` script after the one-shot is confirmed complete.

External cleanup:

```
- Cloudflare dashboard: delete Worker deployment
- Cloudflare dashboard: delete STORE_KV namespace (after verifying Upstash data preserved)
- Vercel dashboard: archive `store-scraper` project (the old wrapper) OR mark README deprecated
- GitHub `tiennm99/store-scraper` repo: add deprecation banner to README pointing to bot repo
```

## Related Code Files

- Delete: `wrangler.toml`
- Delete: `Dockerfile`
- Delete: `docker-compose.yml`
- Delete: `docker-compose.dev.yml`
- Delete: `scripts/migrate-atlas-to-upstash.js`
- Modify: `package.json` (drop `wrangler`, `mongodb`; drop `migrate*` scripts)
- Modify: `README.md`

## Implementation Steps

1. Wait ≥7 days post-Phase 6 with no incidents.
2. Delete `wrangler.toml`.
3. Delete Docker artifacts (no longer needed — Vercel + Upstash REST has no local-Mongo path):
   ```sh
   git rm Dockerfile docker-compose.yml docker-compose.dev.yml
   ```
4. Delete `scripts/migrate-atlas-to-upstash.js` (one-shot done; keep migration logic in plan history).
5. Remove from `package.json`:
   - devDeps: `wrangler`, `mongodb`
   - scripts: `migrate`, `migrate:dry`, `migrate:bulk` (if any remain)
6. Rewrite `README.md`:
   - Replace "Cloudflare Workers" with "Vercel"
   - Update env var table (drop MONGODB_URI / STORE_KV; add UPSTASH_REDIS_REST_URL / TOKEN, CRON_SECRET)
   - Update Run section: `vercel dev` for local; `vercel deploy --prod` for production
   - Drop "Migrating from MongoDB Atlas" section (one-shot completed)
   - Drop "Or via Docker Compose" section
7. Update `.vercelignore` — remove now-deleted Dockerfile/docker-compose entries (no longer need to ignore what doesn't exist).
8. Cloudflare dashboard (only if any CF resources were ever provisioned): `wrangler delete` Worker, then delete STORE_KV namespace. Skip if CF was never deployed.
9. `tiennm99/store-scraper` repo on GitHub:
   - Add README banner: "⚠️ Deprecated — logic now inlined in [`tiennm99/store-scraper-bot`](https://github.com/tiennm99/store-scraper-bot)"
   - Optionally archive the repo (`gh repo archive tiennm99/store-scraper`)
10. Vercel dashboard: delete `store-scraper` project (the old scraper wrapper).
11. Commit: `chore: remove cloudflare + docker + legacy migration scripts`

## Success Criteria

- [ ] `git grep -i 'cloudflare\|wrangler\|store_kv\|docker'` returns only `plans/` history hits
- [ ] `package.json` has no `wrangler` or `mongodb` deps; no `migrate*` scripts
- [ ] README accurately describes Vercel + Upstash; no Docker section
- [ ] `Dockerfile`, `docker-compose.yml`, `docker-compose.dev.yml` removed from repo
- [ ] `tiennm99/store-scraper` repo marked deprecated and/or archived
- [ ] Old Vercel `store-scraper` project deleted

## Risk Assessment

- **Risk:** CF KV namespace was actually populated and Upstash differs. **Mitigation:** N/A if CF was never deployed (per Atlas-source migration in Phase 5). If CF *was* deployed before plan supersession, manually compare key counts before deleting namespace.
- **Risk:** Other consumers exist for `store-scraper.vercel.app` (third-party users). **Mitigation:** add a 410 Gone response on the old wrapper for 14 days before deleting; deprecation banner in repo gives outside heads-up.
- **Risk:** Someone runs the bot locally via Docker for dev. **Mitigation:** explicitly confirmed not needed (Vercel + Upstash REST replaces local-Mongo dev path); `vercel dev` is the new local entry. Document this in README.
- **Risk:** Java bot still running consumes Atlas connections after this phase. **Mitigation:** out of scope for this repo; coordinate with Java repo retirement separately.
