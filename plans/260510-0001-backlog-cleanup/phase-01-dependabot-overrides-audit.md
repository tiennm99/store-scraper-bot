---
phase: 1
title: Dependabot overrides + audit
status: completed
priority: P1
effort: 30m
dependencies: []
---

# Phase 1: Dependabot overrides + audit

## Overview

All 4 open alerts come transitively from `app-store-scraper@0.18.0 → request@2.88.2`. The `request` lib is abandoned (no patched version), but its transitive deps `qs`, `form-data`, `tough-cookie` have patches. Use npm `overrides` to force-pin the patched versions; accept the unfixable `request` SSRF advisory.

## Context (from preflight)

```
app-store-scraper@0.18.0          (latest; still pulls request)
└─ request@2.88.2                 (abandoned — SSRF GHSA, no fix)
   ├─ form-data@2.3.3             → patch to ^2.5.4 (CRITICAL: unsafe random boundary)
   ├─ qs@6.5.5                    → patch to ^6.14.1 (DoS via memory exhaustion)
   └─ tough-cookie@2.5.0          → patch to ^4.1.3 (Prototype Pollution)
google-play-scraper@10.1.2
└─ tough-cookie@4.1.4             (already patched, no action)
```

The `request` SSRF alert has `fixed_in: null` — no upstream fix. Risk: low — we only call known endpoints (`itunes.apple.com`, `play.google.com`); no user-controlled URLs reach `request`. Document + dismiss in GitHub UI as "won't fix / risk-accepted".

## Architecture

`package.json` `overrides` field is npm's documented mechanism for forcing transitive dep versions. Apply at the top level (no nesting) — both `request` and `google-play-scraper` should use the patched versions.

## Related Code Files

**Modify**
- `package.json` — add `overrides` block
- `package-lock.json` — regenerate

## Implementation Steps

1. Add `overrides` to `package.json`:
   ```json
   "overrides": {
     "form-data": "^2.5.4",
     "qs": "^6.14.1",
     "tough-cookie": "^4.1.3"
   }
   ```
   Carets so future patch bumps flow through.
2. Run `npm install` to regenerate `package-lock.json`.
3. Run `npm audit` — expect only the `request` SSRF alert remaining (unfixable). Everything else should clear.
4. `npm ls form-data qs tough-cookie` — confirm overridden versions are in the tree.
5. `npm run lint` to verify no incidental break.
6. Open GitHub Dependabot UI and dismiss the `request` SSRF alert: "risk accepted, no user-controlled URLs reach `request`".

## Success Criteria

- [ ] `npm audit` shows 0 vulnerabilities except the unfixable `request` SSRF
- [ ] `npm ls form-data` shows `^2.5.4`
- [ ] `npm ls qs` shows `^6.14.1`
- [ ] `npm ls tough-cookie` shows `^4.1.3` everywhere
- [ ] GitHub Dependabot page shows 0 open alerts (or only the dismissed `request` one)
- [ ] Bot smoke-run on Vercel preview deploy still works

## Risk Assessment

- **Override breaks app-store-scraper:** lib's pinned `qs@~6.5.2` could rely on old behavior. `qs@6.5 → 6.14` is minor under semver but `qs` has had behavior tweaks (parameter parsing). Mitigation: smoke-test `/checkapp` against a real Apple app after install. If broken, narrow override to a patched 6.5.x line if one exists; else vendor a minimal apple-scraper using native `fetch`.
- **form-data 2.3 → 2.5 bump:** patch-level under semver. Same mitigation.
- **`request` SSRF unfixable:** documented + dismissed. Long-term: replace `app-store-scraper` with in-house fetch wrapper (out of scope).
