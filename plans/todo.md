# Outstanding Work

Bot live on Vercel + Upstash. Java bot retired.

## Backlog

- [ ] Tests (no framework chosen yet — needs its own multi-phase plan)

## Done (see git history)

- ~~Triage Dependabot alerts~~ — overrides pinned in `package.json`; `request` SSRF risk-accepted (260510-0001)
- ~~CI workflow~~ — `.github/workflows/ci.yml` (lint + syntax check) (260510-0001)
- ~~Telegram bot description~~ — `npm run describe` (260510-0001)
- ~~Operations docs~~ — README "Operations" section covers rotation + dashboards (260510-0001)

## Dropped (YAGNI)

- Observability dashboard — Vercel + Upstash dashboards already cover function logs, cron history, Redis metrics. Revisit only when a real ops question can't be answered by the built-ins.
