import { createLogger } from './logger.js';

function parseAdminIds(raw) {
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    .map((s) => Number.parseInt(s, 10))
    .filter((n) => Number.isFinite(n));
}

// Builds config from a Workers `env` binding. Called once per fetch / scheduled
// invocation; cheap.
export function loadConfig(env) {
  const required = [
    'TELEGRAM_BOT_TOKEN',
    'TELEGRAM_BOT_USERNAME',
    'TELEGRAM_WEBHOOK_SECRET',
    'MONGODB_URI',
    'ADMIN_IDS',
  ];
  for (const k of required) {
    if (!env[k]) throw new Error(`${k} is required`);
  }

  const adminIds = parseAdminIds(env.ADMIN_IDS);
  if (adminIds.length === 0) throw new Error('at least one admin ID is required');

  return {
    telegramBotToken: env.TELEGRAM_BOT_TOKEN,
    telegramBotUsername: env.TELEGRAM_BOT_USERNAME,
    telegramWebhookSecret: env.TELEGRAM_WEBHOOK_SECRET,
    adminIds,
    creatorId: adminIds[0],
    isAdmin: (userId) => adminIds.includes(Number(userId)),
    appCacheSeconds: Number(env.APP_CACHE_SECONDS ?? 600),
    numDaysWarningNotUpdated: Number(env.NUM_DAYS_WARNING_NOT_UPDATED ?? 30),
    timezone: 'Asia/Ho_Chi_Minh',
    logger: createLogger(),
  };
}
