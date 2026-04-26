import 'dotenv/config';
import { createLogger } from './logger.js';

const DEFAULT_DATABASE_NAME = 'store-scraper-bot';

function getEnv(key, fallback = '') {
  const v = process.env[key];
  return v && v.length > 0 ? v : fallback;
}

function getEnvInt(key, fallback) {
  const v = process.env[key];
  if (!v) return fallback;
  const n = Number.parseInt(v, 10);
  return Number.isFinite(n) ? n : fallback;
}

function parseAdminIds(raw) {
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    .map((s) => {
      try {
        return BigInt(s);
      } catch {
        return null;
      }
    })
    .filter((v) => v !== null);
}

// Mirrors Java/Go databaseFromURI: extract db name from connection string.
function databaseFromUri(uri) {
  const idx = uri.indexOf('://');
  let rest = idx >= 0 ? uri.slice(idx + 3) : uri;
  const slash = rest.indexOf('/');
  if (slash < 0) return DEFAULT_DATABASE_NAME;
  let tail = rest.slice(slash + 1);
  const q = tail.indexOf('?');
  if (q >= 0) tail = tail.slice(0, q);
  tail = tail.trim();
  return tail.length > 0 ? tail : DEFAULT_DATABASE_NAME;
}

export function loadConfig() {
  const telegramBotToken = getEnv('TELEGRAM_BOT_TOKEN');
  if (!telegramBotToken) throw new Error('TELEGRAM_BOT_TOKEN is required');
  const telegramBotUsername = getEnv('TELEGRAM_BOT_USERNAME');
  if (!telegramBotUsername) throw new Error('TELEGRAM_BOT_USERNAME is required');

  // Java parity: prefer MONGODB_CONNECTION_STRING, fall back to MONGO_URI.
  const mongoUri = getEnv('MONGODB_CONNECTION_STRING', getEnv('MONGO_URI', 'mongodb://localhost:27017'));
  const mongoDatabase = getEnv('MONGO_DATABASE') || databaseFromUri(mongoUri);
  const mongoTimeoutMs = getEnvInt('MONGO_TIMEOUT_SECONDS', 10) * 1000;

  const env = getEnv('ENV', 'DEVELOPMENT') === 'PRODUCTION' ? 'PRODUCTION' : 'DEVELOPMENT';

  const adminIdsRaw = getEnv('ADMIN_IDS');
  if (!adminIdsRaw) throw new Error('ADMIN_IDS is required');
  const adminIds = parseAdminIds(adminIdsRaw);
  if (adminIds.length === 0) throw new Error('at least one admin ID is required');

  const config = {
    telegramBotToken,
    telegramBotUsername,
    mongoUri,
    mongoDatabase,
    mongoTimeoutMs,
    env,
    adminIds,
    creatorId: adminIds[0],
    sourceCommit: getEnv('SOURCE_COMMIT', 'unknown'),
    appCacheSeconds: getEnvInt('APP_CACHE_SECONDS', 600),
    numDaysWarningNotUpdated: getEnvInt('NUM_DAYS_WARNING_NOT_UPDATED', 30),
    scheduleCheckAppTime: getEnv('SCHEDULE_CHECK_APP_TIME', '0 7 * * *'),
    timezone: 'Asia/Ho_Chi_Minh',
    logger: createLogger(env),
  };

  config.isAdmin = (userId) => {
    const id = typeof userId === 'bigint' ? userId : BigInt(userId);
    return config.adminIds.some((a) => a === id);
  };

  return config;
}
