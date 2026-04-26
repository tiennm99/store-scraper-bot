# Deploy Phase 02 — First Deploy + Hard Gates

## Overview
- **Priority:** P0 — gates can abort the entire plan.
- **Status:** pending
- **Description:** Bundle-size dry-run, real deploy, mongo-ping CPU gate, auto-pause behavior test.

## Implementation Steps

1. **Bundle-size hard gate**:
   ```sh
   npx wrangler deploy --dry-run --outdir=.tmp-deploy
   du -sh .tmp-deploy
   ```
   - **PASS**: ≤ 2.7 MiB. Continue.
   - **FAIL**: > 2.7 MiB. **Stop**. Pivot to Upstash (separate plan; out of scope here).

2. **First real deploy**:
   ```sh
   npx wrangler deploy
   ```
   Capture the URL printed (e.g. `https://js-store-scraper-bot.<account>.workers.dev`).
   Update `.env.deploy` with this `WORKER_URL`.

3. **Add temporary `/__mongo-ping` route** for CPU gate (revert before commit at end of phase):
   - Edit `src/index.js` `fetch` handler — top of method, before path routing:
     ```js
     if (request.method === 'GET' && new URL(request.url).pathname === '/__mongo-ping') {
       const t = Date.now();
       const { db } = await getMongo(env);
       await db.command({ ping: 1 });
       return new Response(JSON.stringify({ wall_ms: Date.now() - t }), {
         headers: { 'Content-Type': 'application/json' },
       });
     }
     ```
   - `npx wrangler deploy`.

4. **Cold-start CPU gate** (operator):
   - Run 5 cold cycles, 10+ min apart:
     ```sh
     curl https://js-store-scraper-bot.<account>.workers.dev/__mongo-ping
     ```
   - Open CF dashboard → Workers → js-store-scraper-bot → Logs / Metrics.
   - Note CPU time per invocation (separate from wall_ms).
   - **PASS**: cold CPU < 40ms (10ms safety under 50ms cap).
   - **FAIL**: ≥ 40ms. Decide: upgrade to Workers Paid ($5/mo, 50ms baseline → 5min ceiling), or pivot to Upstash.

5. **Auto-pause behavior test**:
   - Atlas UI → cluster → Pause.
   - Wait ~30s for Atlas to actually pause.
   - `curl .../__mongo-ping` → should return 5xx within ~5s, NOT hang.
   - Atlas UI → resume cluster.
   - Verify: error was catchable (logged via `wrangler tail`); not a hang.

6. **Cleanup**: revert `/__mongo-ping` route in `src/index.js`. Re-deploy:
   ```sh
   git checkout src/index.js   # revert the temp ping route
   npx wrangler deploy
   ```

7. **Record baseline** in `docs/using-mongodb.md` (created in Phase 04): cold-ping P95 wall_ms.

## Todo List
- [ ] Bundle ≤ 2.7 MiB
- [ ] First deploy success; URL captured
- [ ] `.env.deploy` `WORKER_URL` set
- [ ] Temporary `/__mongo-ping` deployed
- [ ] Cold CPU < 40ms across 5 cold cycles
- [ ] Auto-pause yields catchable error
- [ ] Temp route reverted + redeployed
- [ ] Baseline P95 captured for docs

## Success Criteria
- `wrangler deploy` clean.
- All hard gates pass.
- Atlas dashboard "Current Connections" stays low (≤ 5) during ping smoke.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Bundle exceeds 2.7 MiB | M | Plan-aborting | Pivot path documented in plan.md |
| Cold CPU > 50ms | M | Plan-aborting on Free | Escalate to Paid; or pivot |
| Atlas connection refused (`0.0.0.0/0` not yet propagated) | L | Medium | Wait 1-2 min; retry |
| Forgot to revert `/__mongo-ping` | L | Low | Step 6 explicit; lint adds no help here |

## Next Steps
- **Blocks:** Phase 03.
- **Unblocks:** Phase 03.
