---
phase: 7
title: "Cleanup wrangler + docs"
status: pending
priority: P2
effort: 30 min
dependencies: [6]
---

# Phase 7: Cleanup wrangler + docs

## Overview

After Vercel deploy is stable (≥7 days post-cutover), remove Cloudflare Workers artifacts, retire the old `tiennm99/store-scraper` Vercel deployment, and update README + docs to reflect the new architecture.

## Requirements

- Repo no longer references CF Workers, KV, or `wrangler` (except in `plans/` archive history)
- README accurately describes Vercel + Upstash setup
- Old `migrate-atlas-to-kv.js` and `migrate-cf-kv-to-upstash.js` scripts removed (one-shot, role done)
- `mongodb` devDep removed
- `tiennm99/store-scraper` repo marked deprecated

## Architecture

Files to remove:

```
DELETE
├── wrangler.toml
├── docker-compose.yml
├── docker-compose.dev.yml         (if Worker-specific; keep if generic Node)
├── Dockerfile                     (likely Worker-specific; verify)
├── scripts/migrate-atlas-to-kv.js
├── scripts/migrate-cf-kv-to-upstash.js
└── (CF Worker on dashboard) wrangler delete

MODIFY
├── package.json                   remove wrangler, mongodb devDeps
├── README.md                      Vercel + Upstash setup, drop Atlas/KV history
└── docs/                          if any architecture docs reference CF
```

External cleanup:

```
- Cloudflare dashboard: delete Worker deployment
- Cloudflare dashboard: delete STORE_KV namespace (after verifying Upstash data preserved)
- Vercel dashboard: archive `store-scraper` project (the old wrapper) OR mark README deprecated
- GitHub `tiennm99/store-scraper` repo: add deprecation banner to README pointing to bot repo
```

## Related Code Files

- Delete: `wrangler.toml`
- Delete: `scripts/migrate-atlas-to-kv.js`
- Delete: `scripts/migrate-cf-kv-to-upstash.js`
- Delete: `Dockerfile`, `docker-compose*.yml` (verify first; may keep if generic)
- Modify: `package.json` (drop `wrangler`, `mongodb`; drop `migrate*` scripts)
- Modify: `README.md`

## Implementation Steps

1. Wait ≥7 days post-Phase 6 with no incidents.
2. Delete `wrangler.toml`.
3. Delete `scripts/migrate-atlas-to-kv.js` and `scripts/migrate-cf-kv-to-upstash.js`.
4. Remove from `package.json`:
   - devDeps: `wrangler`, `mongodb`
   - scripts: `migrate`, `migrate:bulk`, `migrate:upstash`
5. Verify `Dockerfile` + `docker-compose*.yml` — if Worker-specific, delete; if generic Node + Upstash, keep.
6. Rewrite `README.md`:
   - Replace "Cloudflare Workers" with "Vercel"
   - Update env var table (drop MONGODB_URI / STORE_KV; add UPSTASH_REDIS_REST_URL / TOKEN, CRON_SECRET)
   - Update Run section: `vercel dev` for local; `vercel deploy --prod` for production
   - Drop "Migrating from MongoDB Atlas" section (one-shot completed)
7. Cloudflare dashboard: `wrangler delete` Worker, then delete STORE_KV namespace.
8. `tiennm99/store-scraper` repo on GitHub:
   - Add README banner: "⚠️ Deprecated — logic now inlined in [`tiennm99/store-scraper-bot`](https://github.com/tiennm99/store-scraper-bot)"
   - Optionally archive the repo (`gh repo archive tiennm99/store-scraper`)
9. Vercel dashboard: delete `store-scraper` project (the old wrapper).
10. Commit: `chore: remove cloudflare workers + retire old scraper wrapper`

## Success Criteria

- [ ] `git grep -i 'cloudflare\|wrangler\|store_kv'` returns only `plans/` history hits
- [ ] `package.json` has no `wrangler` or `mongodb`
- [ ] README accurately describes Vercel + Upstash
- [ ] CF Worker no longer exists on dashboard
- [ ] `tiennm99/store-scraper` repo marked deprecated and/or archived
- [ ] Old Vercel `store-scraper` project deleted

## Risk Assessment

- **Risk:** Deleting CF KV namespace before verifying Upstash holds all production data. **Mitigation:** explicitly verify counts in Phase 6 success criteria; defer namespace delete by another 7 days if paranoid.
- **Risk:** Other consumers exist for `store-scraper.vercel.app` (third-party users). **Mitigation:** check by adding a 410 Gone response for 14 days before deleting; deprecation banner in repo gives outside heads-up.
- **Risk:** `Dockerfile` deletion breaks an alternate deploy path the user relies on. **Mitigation:** ask user before deleting; if uncertain, keep file with a comment.
