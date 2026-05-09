// Per-invocation context wiring. Both Vercel handlers (api/webhook.js,
// api/cron.js) call this once per request. Cheap — the Upstash client is
// HTTP-based, so no socket/connection setup happens until the first command.

import { loadConfig } from './config.js';
import { createUpstashClient } from './repository/upstash.js';
import { createAdminRepository } from './repository/admin-repository.js';
import { createGroupRepository } from './repository/group-repository.js';
import { createAppCacheRepository } from './repository/app-cache-repository.js';
import { createAppleScraper } from './api/apple-scraper.js';
import { createGoogleScraper } from './api/google-scraper.js';
import { createBot } from './bot/bot.js';

export function buildApp(env) {
  const config = loadConfig(env);
  const handle = createUpstashClient(env);
  const store = {
    admin: createAdminRepository(handle),
    group: createGroupRepository(handle),
  };
  const appleCache = createAppCacheRepository(handle, 'apple', config.appCacheSeconds);
  const googleCache = createAppCacheRepository(handle, 'google', config.appCacheSeconds);
  const appleScraper = createAppleScraper(config, appleCache);
  const googleScraper = createGoogleScraper(config, googleCache);
  const { sender, commands } = createBot(config, store, appleScraper, googleScraper);
  return { config, store, appleScraper, googleScraper, sender, commands };
}
