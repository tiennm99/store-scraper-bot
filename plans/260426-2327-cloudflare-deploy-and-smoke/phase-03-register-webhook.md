# Deploy Phase 03 — Register Webhook

## Overview
- **Priority:** P1
- **Status:** pending
- **Description:** Run the script written in code-plan Phase 05 against the live URL.

## Implementation Steps

1. Verify `.env.deploy` has `WORKER_URL` set (from Deploy Phase 02 step 2).
2. Dry-run first:
   ```sh
   npm run register:dry
   ```
   Verify printed payloads look right (URL, secret, command list).
3. Real run:
   ```sh
   npm run register
   ```
4. Verify `getWebhookInfo` output in script's stdout shows:
   - `url`: matches `WORKER_URL`
   - `pending_update_count`: 0
   - `last_error_date`: absent
5. In Telegram client, type `/` in a chat with the bot — autocompletion should show the 13 commands (within ~1 min of `setMyCommands`).

## Todo List
- [ ] `npm run register:dry` clean
- [ ] `npm run register` clean
- [ ] `getWebhookInfo` shows correct URL + 0 pending
- [ ] Telegram `/` autocompletion shows 13 commands

## Success Criteria
- All checks above.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Wrong WORKER_URL | M | Medium | Dry-run catches this |
| Telegram returns `wrong response from the webhook` | L | High | Means Worker is reachable but returns non-200; check `wrangler tail` |

## Next Steps
- **Blocks:** Phase 04.
- **Unblocks:** Phase 04.
