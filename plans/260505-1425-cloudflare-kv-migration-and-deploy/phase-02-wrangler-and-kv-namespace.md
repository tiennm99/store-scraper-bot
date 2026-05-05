---
phase: 2
title: "Wrangler config + KV namespace provisioning"
status: pending
priority: P1
effort: "0.5h"
dependencies: [1]
---

# Phase 02: Wrangler config + KV namespace provisioning

## Overview

Provision a Cloudflare KV namespace, bind it to the Worker as `STORE_KV`, and remove now-unused MongoDB-related config from `wrangler.toml`.

## Requirements

**Functional**
- Worker has access to `env.STORE_KV` at runtime.
- Local `wrangler dev` reads from a preview namespace, not production.

**Non-functional**
- `wrangler.toml` is the single source of truth (no `wrangler.jsonc`).
- Production and preview namespace IDs are committed (these are not secrets).

## Architecture

KV bindings are declared in `wrangler.toml`:

```toml
[[kv_namespaces]]
binding = "STORE_KV"
id = "<production namespace id>"
preview_id = "<preview namespace id>"
```

`binding` is the name surfaced as `env.STORE_KV` in the Worker. `id` is the production namespace UUID. `preview_id` is used by `wrangler dev`.

Operator-driven steps (run locally; require `wrangler login`):

```sh
npx wrangler kv namespace create STORE_KV
npx wrangler kv namespace create STORE_KV --preview
```

Each command outputs a TOML snippet to copy into `wrangler.toml`.

## Related Code Files

**Modify**
- `wrangler.toml` — add `[[kv_namespaces]]` block; remove `nodejs_compat_v2` flag; trim the secrets-comment block to drop `MONGODB_URI`.

**No code changes** — Phase 01 already wrote everything that consumes `env.STORE_KV`.

## Implementation Steps

1. **Run** `npx wrangler kv namespace create STORE_KV` — capture the production `id`.
2. **Run** `npx wrangler kv namespace create STORE_KV --preview` — capture the `preview_id`.
3. **Edit `wrangler.toml`**:
   - Add the `[[kv_namespaces]]` block with both IDs.
   - Change `compatibility_flags` from `["nodejs_compat_v2"]` to `[]` (or delete the line entirely).
   - In the `# Secrets` comment block, remove the `MONGODB_URI` line.
4. **Verify** with `npx wrangler types` (if available) or `npx wrangler deploy --dry-run` — should report the new binding without errors.
5. **Local seed (optional)**: For `wrangler dev` to be useful, the admin singleton must exist. The repository's `init()` is currently called nowhere automatically. Either:
   - Seed manually: `npx wrangler kv key put --binding STORE_KV --preview admin '{"_id":"admin","class":"Admin","groups":[]}'`
   - Or update `src/index.js` to call `store.admin.init()` on first request (cheap, idempotent). **Recommended**: add the lazy init.

## Todo List

- [ ] `wrangler kv namespace create STORE_KV`
- [ ] `wrangler kv namespace create STORE_KV --preview`
- [ ] Add `[[kv_namespaces]]` block to `wrangler.toml`
- [ ] Drop `nodejs_compat_v2` from `compatibility_flags`
- [ ] Remove `MONGODB_URI` mention from the secrets comment
- [ ] Add lazy `store.admin.init()` on first request in `src/index.js`
- [ ] `wrangler deploy --dry-run` succeeds

## Success Criteria

- [ ] `wrangler.toml` has `[[kv_namespaces]]` with `binding = "STORE_KV"` and both IDs.
- [ ] `compatibility_flags` no longer contains `nodejs_compat_v2`.
- [ ] `wrangler deploy --dry-run` exits 0 and lists the binding.
- [ ] `wrangler kv namespace list` shows two namespaces: `STORE_KV` and `STORE_KV_preview` (or named per CF convention).

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Wrong `binding` name → `env.STORE_KV` is undefined at runtime | Low | Phase 01 added the early validation throw |
| Forgot `preview_id`, `wrangler dev` errors | Low | Step 2 explicitly creates it |
| Operator commits secret accidentally | Low | KV IDs are not secrets; existing `npm run lint` scans for the secret patterns we care about |

## Security Considerations

- KV namespace IDs are not secrets and are safe to commit.
- Production `STORE_KV` will hold Telegram chat IDs and admin IDs — same sensitivity as Mongo previously held. Cloudflare KV is encrypted at rest.
