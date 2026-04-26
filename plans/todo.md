# Outstanding Work

Quick index of what's left after the Worker code port (commit `bff1d32`).

## Active plan

**[260426-2327-cloudflare-deploy-and-smoke](260426-2327-cloudflare-deploy-and-smoke/plan.md)** — operator-driven; provision Atlas, set CF secrets, run hard gates, deploy, register webhook, smoke-test, write deployment docs.

## Pre-flight (operator)

Before running the deploy plan:

- [ ] `npm install` (first time only — pulls `mongodb` + `wrangler`)
- [ ] MongoDB Atlas account created (free, no credit card)
- [ ] Cloudflare account created (free Workers plan)
- [ ] `npx wrangler login` complete

## Hard gates that can abort the deploy plan

| Gate | Threshold | Pivot if fail |
|---|---|---|
| Bundle size (`wrangler deploy --dry-run`) | ≤ 2.7 MiB (10% headroom under 3 MiB Free cap) | Switch storage to Upstash Redis (no driver, HTTP-only) |
| Cold-start CPU (`/__mongo-ping`) | < 40ms (10ms headroom under 50ms Free cap) | Workers Paid ($5/mo) OR pivot to Upstash |
| Atlas auto-pause | Catchable error within 5s, not a hang | Document catch path; not abort-worthy |

`mongodb` driver is the dominant risk — bundles ~4–5 MiB compressed against the 3 MiB Free cap. miti99bot has the same risk and has not yet validated the gate end-to-end on real CF Free either.

## Open questions for the operator

1. Atlas account: existing or first-time setup?
2. Bot username for `setMyCommands`: same as `.env.example`?
3. Greenfield Mongo data, or existing data to import? (Existing → schedule a separate import phase.)
4. Custom domain or `*.workers.dev` URL? (Webhook works on either.)

## Backlog (post-deploy, out of current scope)

- [ ] Tests (none exist; original Node port has none either)
- [ ] Quarterly Atlas password rotation reminder
- [ ] Observability dashboard (CF Workers + Atlas charts)
- [ ] CI workflow on push (lint + dry-run bundle size as PR check)
- [ ] Telegram bot description / about-text via `setMyDescription` (post-deploy)

## Reference

- **miti99bot** validated the same stack (CF Workers + Atlas + `nodejs_compat_v2` + `mongodb`): `/config/workspace/tiennm99/miti99bot/plans/260425-1945-mongodb-atlas-migration/`
- **Archived code-port plan** (history of what's now done): [archive/260426-2015-cloudflare-worker-code-port](archive/260426-2015-cloudflare-worker-code-port/plan.md)
