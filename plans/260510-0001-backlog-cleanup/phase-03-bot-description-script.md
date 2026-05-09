---
phase: 3
title: Bot description script
status: completed
priority: P3
effort: 20m
dependencies: []
---

# Phase 3: Bot description script

## Overview

`/setdayswarning` and `/settings` need to also show up in the Telegram command picker — extend the existing `setMyCommands` list. Separately, set the bot's About + Description text via `setMyDescription` and `setMyShortDescription` so users see what it does in the chat header / profile.

## Architecture

Two pieces:
1. **Extend `register-webhook.js`** — add the two new commands (`settings`, `setdayswarning`) to the existing `COMMANDS` array. They'll be pushed on the next `npm run register`.
2. **New one-shot script** `scripts/set-bot-description.js` — calls `setMyDescription` (long) + `setMyShortDescription` (140 chars), gated by env. Run manually after copy is locked.

Description copy (English; bot is operated in Vietnam but commands are English in the existing register script):

- **Short** (≤120 chars): `Track Apple App Store + Google Play app updates. Get notified when tracked apps go N days without an update.`
- **Long** (≤512 chars): two paragraphs explaining tracking + the daily report + how to get started (`/info` → ask admin to `/addgroup` → `/addapple` etc.).

## Related Code Files

**Modify**
- `scripts/register-webhook.js` — append `settings` + `setdayswarning` to `COMMANDS`

**Create**
- `scripts/set-bot-description.js` — modeled on `register-webhook.js` (same env loading, same `--dry-run` flag)

**Modify**
- `package.json` `scripts` — add `"describe": "node --env-file=.env.deploy scripts/set-bot-description.js"`

## Implementation Steps

1. Read existing `scripts/register-webhook.js` to confirm env-loading pattern (`.env.deploy` via `--env-file`).
2. Append to `COMMANDS` array in `register-webhook.js`:
   ```js
   { command: 'settings', description: 'Show this group\'s settings' },
   { command: 'setdayswarning', description: 'Set warning threshold (days)' },
   ```
3. Create `scripts/set-bot-description.js`:
   - Same env-required check as register-webhook (just `TELEGRAM_BOT_TOKEN`)
   - Defines `SHORT` and `LONG` string constants
   - Posts to `https://api.telegram.org/bot{TOKEN}/setMyDescription` with `{ description: LONG }`
   - Posts to `…/setMyShortDescription` with `{ short_description: SHORT }`
   - Honors `--dry-run` like the webhook script
   - Logs the resulting bot info via `getMe` for confirmation
4. Add `describe` npm script.
5. Test: `npm run register` (pushes new commands), then `npm run describe` (sets descriptions). Confirm in Telegram.

## Success Criteria

- [ ] Telegram command picker in any group/DM shows `settings` and `setdayswarning`
- [ ] BotFather-equivalent description is set (visible when user opens bot profile)
- [ ] Both scripts honor `--dry-run`
- [ ] `npm run describe` is idempotent (Telegram allows re-setting same value)

## Risk Assessment

- **Description length limits:** Telegram caps short at 120, long at 512. Mitigation: hard-coded copy known to fit; script can `assert` length before posting.
- **Wrong language:** existing copy is English; if user wants Vietnamese, the script supports per-language via `language_code` param. Not adding now (YAGNI).
- **No rollback:** setting is global per bot. To revert, re-run with previous text. Acceptable.
