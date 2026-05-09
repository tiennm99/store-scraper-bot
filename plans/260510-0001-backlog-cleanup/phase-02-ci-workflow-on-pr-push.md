---
phase: 2
title: CI workflow on PR/push
status: completed
priority: P2
effort: 30m
dependencies: []
---

# Phase 2: CI workflow on PR/push

## Overview

Add a single GitHub Actions workflow running on PR + push: `npm ci`, secret-leak lint, and `node --check` on every `.js` file. No tests exist yet so no test job. Cheap signal that someone's commit at least parses + doesn't add secrets.

## Architecture

One workflow file, single job, two minutes max runtime. Use Node 20 (matches `engines.node` in `package.json`). Cache npm via `actions/setup-node@v4`'s built-in cache.

## Related Code Files

**Create**
- `.github/workflows/ci.yml`

## Implementation Steps

1. Create `.github/workflows/ci.yml`:
   ```yaml
   name: CI
   on:
     push:
       branches: [main]
     pull_request:
   jobs:
     verify:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - uses: actions/setup-node@v4
           with:
             node-version: 20
             cache: npm
         - run: npm ci
         - run: npm run lint
         - name: Syntax check all JS
           run: find api scripts src -name '*.js' -print0 | xargs -0 -n1 node --check
   ```
2. Commit + push → confirm green run in Actions tab.
3. Add a status badge to README (optional, low value — skip unless trivial).

## Success Criteria

- [ ] Workflow file present at `.github/workflows/ci.yml`
- [ ] First run on push to `main` is green
- [ ] PRs show CI status check
- [ ] Job completes < 2 minutes

## Risk Assessment

- **Bundle-size gate dropped:** original todo asked for "lint + bundle-size as PR check". Bundle size mattered when target was Cloudflare Workers (1MB script limit). On Vercel serverless, 250MB unzipped limit makes bundle size irrelevant. Skipped — YAGNI.
- **`find` path coverage:** if a future top-level dir is added, the syntax check misses it. Mitigation: documented in workflow comment to update the find roots when adding new dirs. Acceptable — no current dynamism in repo layout.
- **`npm ci` slowness:** mitigated by built-in npm cache. Cold runs ~30s, warm <10s.
