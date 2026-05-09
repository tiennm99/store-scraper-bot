---
phase: 1
title: Schema + threshold resolver
status: completed
priority: P2
effort: 30m
dependencies: []
---

# Phase 1: Schema + threshold resolver

## Overview

Extend the per-group state with an optional `settings` object and add one helper that resolves the effective warning threshold. Replace direct `config.numDaysWarningNotUpdated` reads at all call sites.

## Architecture

**Storage shape (Upstash key `group:{chatId}`)**
```js
{ appleApps: [...], googleApps: [...], settings: { numDaysWarningNotUpdated?: number } }
```
- `settings` is optional. Missing → behaves identically to today.
- Additive change; no migration needed.

**Resolver (single source of truth)**
```js
// src/util/group-settings.js
export function resolveDaysWarning(group, config) {
  const v = group?.settings?.numDaysWarningNotUpdated;
  return Number.isFinite(v) && v > 0 ? v : config.numDaysWarningNotUpdated;
}
```

**Repository surface**
Add to `createGroupRepository`:
```js
async function setSetting(groupId, key, value) {
  return mutateAndSave(groupId, (g) => {
    g.settings ??= {};
    if (value === undefined || value === null) delete g.settings[key];
    else g.settings[key] = value;
    return true;
  });
}
```
- Generic so `/setdayswarning` and any future setter share one path.
- `emptyGroup()` does NOT add `settings: {}` — keep payload minimal; resolver tolerates absence.

## Related Code Files

**Create**
- `src/util/group-settings.js` — `resolveDaysWarning(group, config)`

**Modify**
- `src/repository/group-repository.js` — add `setSetting` to public surface
- `src/scheduler/scheduler.js` — replace `config.numDaysWarningNotUpdated` reads (lines 39, 114) with `resolveDaysWarning(group, config)`
- `src/bot/commands/check-app.js` — replace `config.numDaysWarningNotUpdated` (line 15) with resolver

## Implementation Steps

1. Create `src/util/group-settings.js` with `resolveDaysWarning`.
2. In `group-repository.js`, add `setSetting` mutator + export it.
3. In `scheduler.js#checkGroup`, after `await store.group.getGroup(groupId)`, compute `const threshold = resolveDaysWarning(group, config);` — drop the `config.numDaysWarningNotUpdated` ref.
4. In `scheduler.js#buildReport`, accept the resolved threshold (pass it through) and use it in the header string instead of `config.numDaysWarningNotUpdated`.
5. In `check-app.js`, swap line 15 to `const threshold = resolveDaysWarning(group, config);`.
6. `node --check` all four touched files.

## Success Criteria

- [ ] Resolver returns env default when `group.settings` absent
- [ ] Resolver returns group override when valid integer > 0 stored
- [ ] Resolver ignores zero / negative / non-finite stored values (falls back)
- [ ] Scheduler + `/checkapp` use resolved value at every call site
- [ ] Daily report header shows the per-group threshold, not the global default

## Risk Assessment

- **Header drift:** scheduler builds report header with global default today. If we forget to thread the resolved value into `buildReport`, groups with overrides see a misleading header. Mitigation: pass the resolved threshold as a fn arg, not a closure.
- **Bad stored values:** if a future bug writes a string/null, resolver must not crash. Mitigation: `Number.isFinite && > 0` guard.
- **Schema rot:** `emptyGroup()` not adding `settings` keeps payload minimal but means every reader must tolerate undefined. Mitigation: only the resolver reads it; `setSetting` lazily inits.
