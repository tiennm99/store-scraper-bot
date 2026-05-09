---
phase: 2
title: Commands + wiring
status: completed
priority: P2
effort: 45m
dependencies:
  - 1
---

# Phase 2: Commands + wiring

## Overview

Add `/settings` (read) and `/setdayswarning` (write) commands. Wire both into the dispatcher map in `bot.js`. Both gate on `authorizeGroup` like every other command except `/info`.

## Architecture

**Command names**
- `/settings` — generic reader, prints all settings + their effective values. Future settings drop in here.
- `/setdayswarning <n>` — single-purpose setter. Pattern: one setter command per setting key. Easier UX than a generic `/set <key> <value>`.

**Reset semantics for `/setdayswarning`**
- `<n>` parsed as integer
- `n >= 1 && n <= 3650` → store
- `n == 0` or arg `default` → reset (delete the setting key, fall back to env default)
- Anything else → "Invalid arguments"

**`/settings` output**
```
<b>Group Settings</b>
<pre>┌──────────────────────────┬───────┬─────────┐
│ Setting                  │ Value │ Default │
├──────────────────────────┼───────┼─────────┤
│ numDaysWarningNotUpdated │ 45    │ 30      │
└──────────────────────────┴───────┴─────────┘</pre>
```
Use existing `buildTable` from `src/util/table.js`. Show env default in a separate column so user understands what `0`/reset means.

## Related Code Files

**Create**
- `src/bot/commands/get-settings.js` — `createGetSettingsCommand(config, store)` returns handler
- `src/bot/commands/set-days-warning.js` — `createSetDaysWarningCommand(config, store)` returns handler

**Modify**
- `src/bot/bot.js` — import + register both:
  ```js
  settings: createGetSettingsCommand(config, store),
  setdayswarning: createSetDaysWarningCommand(config, store),
  ```

## Implementation Steps

1. Create `src/bot/commands/get-settings.js`:
   - Signature `(msg, sender, args) => …`
   - First line: `if (!(await authorizeGroup(msg.chat.id, store, sender))) return;`
   - Reject if `args.length !== 0`
   - Read group via `store.group.getGroup(msg.chat.id)`
   - Build settings rows: known keys list `[['numDaysWarningNotUpdated', g?.settings?.numDaysWarningNotUpdated, config.numDaysWarningNotUpdated]]`
   - Render table with `buildTable`
   - Send via `sender.sendMessage`

2. Create `src/bot/commands/set-days-warning.js`:
   - Same auth guard
   - Reject if `args.length !== 1`
   - Parse arg: `Number.parseInt`
   - If `args[0] === 'default'` or parsed `=== 0` → call `store.group.setSetting(chatId, 'numDaysWarningNotUpdated', undefined)`, reply "Reset to default (Nd)"
   - If parsed valid (`Number.isFinite && >= 1 && <= 3650`) → `setSetting(chatId, 'numDaysWarningNotUpdated', parsed)`, reply "Days-to-warning set to N"
   - Otherwise `sender.sendMessage(chatId, 'Invalid arguments')` and return

3. In `bot.js`:
   - Add two imports near the other command imports
   - Add two map entries in the `commands` object (preserve alphabetical-ish grouping or stick at end with raw\* commands — match existing convention)

4. `node --check` on the three modified/new files.

## Success Criteria

- [ ] `/settings` from authorized group prints table with current value + env default
- [ ] `/settings` from unauthorized group returns "Group is not allowed to use bot"
- [ ] `/setdayswarning 45` persists to Upstash; subsequent `/settings` shows 45
- [ ] `/setdayswarning 0` (or `default`) deletes the setting; resolver falls back to env default
- [ ] `/setdayswarning abc` / `/setdayswarning -5` / `/setdayswarning 9999` → "Invalid arguments"
- [ ] After `/setdayswarning 45`, daily cron + `/checkapp` use 45 for that group only

## Risk Assessment

- **Command discoverability:** users may not know the command exists. Mitigation: out-of-scope (no help command exists in either Java or JS bot). Consider a future `/help` command.
- **Race on concurrent writes:** `mutateAndSave` is read-modify-write without a transaction. Two simultaneous `/setdayswarning` calls in the same group could clobber each other. Acceptable — low likelihood, low blast radius (last write wins on the same key).
- **No admin gate:** any group member can change the threshold. Matches existing model (any member can `/addapple`). If product wants admin-only later, add `requireAdminUser` check — pattern already exists in `command-utils.js`.
