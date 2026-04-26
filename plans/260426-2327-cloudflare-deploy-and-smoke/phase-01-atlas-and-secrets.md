# Deploy Phase 01 — Atlas Provisioning + CF Secrets

## Context Links
- miti99bot Phase 01 (validated): `/config/workspace/tiennm99/miti99bot/plans/260425-1945-mongodb-atlas-migration/phase-01-atlas-setup.md`

## Overview
- **Priority:** P0
- **Status:** pending
- **Description:** One-time provisioning of Atlas M0 cluster + Cloudflare secrets. No code changes.

## Implementation Steps

1. **Atlas account + cluster** (~15 min):
   - Create Atlas account at https://cloud.mongodb.com (no credit card).
   - Create project `js-store-scraper-bot`.
   - Provision M0 in `aws-ap-southeast-1`. Cluster name: `js-store-scraper-bot-prod`.
   - Wait for cluster green status.

2. **DB user**:
   - Atlas → Database Access → Add user.
   - Username: `js-store-scraper-worker`.
   - Password: ≥32 random chars (use `openssl rand -base64 32`). **Save in password manager.**
   - Role: `readWrite@store-scraper-bot`.

3. **Network access**:
   - Atlas → Network Access → Add IP → `0.0.0.0/0`.
   - Comment: "Cloudflare Workers — no static egress IPs on free plan".

4. **Atlas alerts** (free, ~5 min):
   - Project → Alerts → Add Alert.
   - Cluster unavailable → email.
   - Connections > 400 → email.

5. **Connection string**:
   - Atlas → Database → Connect → Drivers (Node 6.7+).
   - Copy SRV string: `mongodb+srv://js-store-scraper-worker:<password>@<host>/store-scraper-bot?retryWrites=true&w=majority`.
   - Substitute the real password.

6. **Cloudflare account + login**:
   - Sign up at cloudflare.com if not already (free).
   - In project dir: `npx wrangler login`. Browser flow to authorize.

7. **Set 5 secrets**:
   ```sh
   cd /config/workspace/tiennm99/js-store-scraper-bot
   npx wrangler secret put TELEGRAM_BOT_TOKEN          # paste your bot token
   npx wrangler secret put TELEGRAM_BOT_USERNAME       # e.g. miti99_store_bot
   npx wrangler secret put TELEGRAM_WEBHOOK_SECRET     # generate: openssl rand -hex 32
   npx wrangler secret put MONGODB_URI                 # paste full SRV string
   npx wrangler secret put ADMIN_IDS                   # e.g. 123456789,987654321
   ```

8. **Mirror to `.env.deploy`** (for the register script — gitignored):
   ```sh
   cp .env.deploy.example .env.deploy
   # edit: paste TELEGRAM_BOT_TOKEN, TELEGRAM_WEBHOOK_SECRET, WORKER_URL
   ```
   `WORKER_URL` won't be known until first deploy in Phase 02; leave placeholder for now.

## Todo List
- [ ] Atlas project + M0 cluster green
- [ ] DB user + password vaulted
- [ ] Network `0.0.0.0/0` added
- [ ] Atlas alerts configured
- [ ] Connection string captured
- [ ] `wrangler login` complete
- [ ] All 5 secrets set
- [ ] `.env.deploy` populated (WORKER_URL TBD)

## Success Criteria
- `npx wrangler secret list` shows all 5 secret names.
- Atlas dashboard shows cluster green + 0 connections.

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Atlas signup takes longer than expected | M | Low | Allocate 30 min |
| Password copied wrong | L | High | Vault first; paste from vault |
| `wrangler secret put` fails | L | Medium | Re-run; check `wrangler whoami` |

## Next Steps
- **Blocks:** Phase 02.
- **Unblocks:** Phase 02.
