# Deploy Phase 04 — End-to-End Smoke + Docs

## Overview
- **Priority:** P1
- **Status:** pending
- **Description:** Real Telegram smoke of all 13 commands, scheduled handler smoke, README + docs update.

## Implementation Steps

1. **Open `wrangler tail`** in a terminal to stream logs.

2. **Smoke checklist** in a Telegram chat with the bot (operator must be in `ADMIN_IDS`):
   - [ ] `/info` → bot replies with chat ID
   - [ ] `/addgroup` → "Group added successfully"
   - [ ] `/addgoogle com.android.chrome vn` → success
   - [ ] `/addapple 284910350 vn` → success (YouTube iOS)
   - [ ] `/listapp` → table with both apps
   - [ ] `/checkapp` → 4-col table with green checks
   - [ ] `/checkappscore` → score table
   - [ ] `/rawappleapp 284910350 vn` → JSON file attachment
   - [ ] `/rawgoogleapp com.android.chrome vn` → JSON file attachment
   - [ ] `/listgroup` → group listed
   - [ ] `/delapple 284910350` → success
   - [ ] `/delgoogle com.android.chrome` → success
   - [ ] `/delgroup` → success

3. **Scheduled smoke**: trigger the cron handler via curl (works on `wrangler dev --test-scheduled`):
   ```sh
   npx wrangler dev --test-scheduled
   # in another terminal:
   curl "http://localhost:8787/__scheduled?cron=0+0+*+*+*"
   ```
   Verify `runDailyCheck` log lines appear.

4. **Atlas check**: dashboard "Current Connections" ≤ 5 throughout smoke.

5. **Write `docs/cloudflare-deployment.md`** — operator runbook:
   - Prerequisites
   - One-time setup commands (Atlas + secrets + first deploy)
   - Routine deploy: `npm run deploy`
   - Reading `wrangler tail`
   - Pause/restart procedure
   - Rollback to Node polling
   - Fallback to Upstash (sketch only — link to plan if it ever runs)

6. **Write `docs/using-mongodb.md`** — Atlas runbook:
   - Cluster URL (without password)
   - Region, alerts, auto-pause schedule
   - Password rotation procedure
   - Baseline cold-ping P95 (from Phase 02 step 7)
   - `0.0.0.0/0` permanence + paid CF static-IP path

7. **Update `README.md`**:
   - Replace existing "Run" section: "Deploys to Cloudflare Workers; see `docs/cloudflare-deployment.md`."
   - Keep preview/untested banner.
   - List new deps: `mongodb`, `wrangler`.
   - Note: Docker compose retained for local-with-Mongo dev only.

8. **Mark Docker files deprecated** — header comment:
   ```
   # DEPRECATED: this project deploys to Cloudflare Workers (see docs/cloudflare-deployment.md).
   # Retained only for local-with-Mongo development.
   ```

9. **Commit + push**:
   ```sh
   git add docs/ README.md Dockerfile docker-compose*.yml
   git commit -m "docs: Cloudflare Workers deployment runbook + smoke complete"
   git push
   ```

## Todo List
- [ ] All 13 commands smoke-tested
- [ ] Scheduled handler smoke OK
- [ ] Atlas connection count low
- [ ] `docs/cloudflare-deployment.md` written
- [ ] `docs/using-mongodb.md` written
- [ ] `README.md` updated
- [ ] Docker files marked deprecated
- [ ] Committed + pushed

## Success Criteria
- All commands respond correctly in real Telegram.
- Operator runbook is complete and someone else could redeploy from scratch following it.
- Daily 7am-VN cron will fire (verify next morning in Atlas op-log + `wrangler tail` history).

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Smoke reveals refactor bug | M | Medium | Fix + redeploy; not catastrophic |
| Cron timezone confusion | L | Low | Documented `0 UTC = 7 VN`; verify next day |

## Next Steps
- **Blocks:** none.
- **Unblocks:** end of plan.
- **Follow-up (out of scope):** tests; observability dashboard; quarterly password rotation reminder.
