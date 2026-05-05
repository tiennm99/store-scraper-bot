---
phase: 5
title: "Docs + cleanup"
status: pending
priority: P2
effort: "1h"
dependencies: [4]
---

# Phase 05: Docs + cleanup

## Overview

Update README + project docs to reflect KV-backed storage, archive the obsolete Atlas plan, and remove now-dead Mongo references from `.env*` examples and Docker files.

## Requirements

**Functional**
- A new operator can deploy from scratch by following only `README.md` + `docs/deployment-guide.md`.
- No file references MongoDB Atlas as the production storage.

**Non-functional**
- Keep `docs/*.md` under 800 LOC each (per `docs.maxLoc`).

## Architecture

Documentation surface:

| File | Action |
|---|---|
| `README.md` | Replace MongoDB section with KV; update env table; bump status note |
| `.env.example` | Remove `MONGODB_*`; add note that secrets live in `wrangler secret put` |
| `.dev.vars.example` | Replace `MONGODB_URI=` with KV preview-namespace seeding hint |
| `docs/deployment-guide.md` | Create or update with the Phase 02–04 operator runbook |
| `docs/system-architecture.md` | Diagram: Worker → KV (drop Atlas) |
| `plans/todo.md` | Remove the Atlas pre-flight items; update active-plan link |
| `plans/260426-2327-cloudflare-deploy-and-smoke/` | Move under `plans/archive/` with a "superseded by 260505-1425" note |

Docker files (`Dockerfile`, `docker-compose.yml`, `docker-compose.dev.yml`) reference the old polling path. **Decision**: keep them as a fallback rollback option but add a banner comment that they target the legacy Mongo runtime, not Workers.

## Related Code Files

**Modify**
- `README.md`
- `.env.example`
- `.dev.vars.example`
- `plans/todo.md`
- `Dockerfile` (banner comment only)
- `docker-compose.yml` (banner comment only)
- `docker-compose.dev.yml` (banner comment only)

**Create**
- `docs/deployment-guide.md` (if missing)
- `docs/system-architecture.md` (if missing) — short, ASCII or mermaid diagram

**Move (archive)**
- `plans/260426-2327-cloudflare-deploy-and-smoke/` → `plans/archive/260426-2327-cloudflare-deploy-and-smoke/`

## Implementation Steps

1. **`README.md`**:
   - Remove "Requirements: MongoDB 4.4+".
   - Update env table: drop `MONGODB_CONNECTION_STRING`, `MONGO_DATABASE`; note that secrets go via `wrangler secret put`, KV binding is `STORE_KV`.
   - Replace "Run via Docker Compose" with "Deploy: `npm run deploy`".
   - Update "Differences vs Go / Java" to note storage backend differs (KV instead of Mongo) but document shape preserved.

2. **`.env.example`**:
   - Drop `MONGODB_*` rows; add a comment that production secrets live in `wrangler secret`.
   - Keep `ADMIN_IDS`, `TELEGRAM_*` for local-dev reference only.

3. **`.dev.vars.example`** — drop `MONGODB_URI`.

4. **`docs/deployment-guide.md`** — short runbook condensing Phases 02–04 (KV namespace creation, secrets, deploy, register webhook, smoke). One file, < 200 lines.

5. **`docs/system-architecture.md`** — mermaid diagram:
   ```
   Telegram --webhook--> CF Worker --KV ops--> STORE_KV
                                  --HTTPS--> iTunes / Play
                          cron --7am VN--> daily check
   ```

6. **`plans/todo.md`**:
   - Replace "Active plan" link with `260505-1425-cloudflare-kv-migration-and-deploy`.
   - Remove the Atlas-specific hard-gate table (no longer applicable).
   - Update pre-flight: drop Atlas account requirement; KV needs only CF account.

7. **Archive old plan**:
   ```sh
   mv plans/260426-2327-cloudflare-deploy-and-smoke plans/archive/
   ```
   Append a single line at top of its `plan.md`: `> **Superseded by [`260505-1425-cloudflare-kv-migration-and-deploy`](../../260505-1425-cloudflare-kv-migration-and-deploy/plan.md)** — KV pivot taken pre-emptively.`

8. **Docker file banners** — one-line comment at top:
   ```
   # NOTE: Legacy Mongo polling path. Production deploys to Cloudflare Workers via `npm run deploy` and uses Cloudflare KV.
   ```

9. **Final commit**: single squashed commit `feat: migrate to Cloudflare KV storage + deploy` with the full Phase 01–04 diff. Use conventional-commits.

## Todo List

- [ ] Update `README.md`
- [ ] Update `.env.example`
- [ ] Update `.dev.vars.example`
- [ ] Create/update `docs/deployment-guide.md`
- [ ] Create/update `docs/system-architecture.md`
- [ ] Update `plans/todo.md`
- [ ] Move old Atlas plan under `plans/archive/`
- [ ] Add legacy banners to Docker files
- [ ] Commit + push

## Success Criteria

- [ ] `grep -rn "MONGODB\|mongodb\|Atlas" README.md docs/ .env.example .dev.vars.example` returns nothing.
- [ ] `docs/deployment-guide.md` exists and is < 200 lines.
- [ ] Old plan lives under `plans/archive/` with the supersession banner.
- [ ] `git log --oneline -5` shows a clean conventional commit.

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Stale Atlas reference left in some doc | Low | grep step in success criteria |
| Doc exceeds `docs.maxLoc=800` | Low | Each doc is short by design |
| Operator follows old plan by accident | Low | Archive move + supersession banner make it obvious |

## Security Considerations

- Confirm no secrets are pasted into README or examples.
- `npm run lint` (the existing `scripts/check-secret-leaks.js`) runs at commit time per existing workflow.

## Next Steps (out of this plan, into backlog)

- CI workflow that runs `wrangler deploy --dry-run` as a PR bundle-size gate.
- Observability dashboard pointed at the `STORE_KV` namespace (request count + key count).
- Tests — original project has none; would be additive work.
