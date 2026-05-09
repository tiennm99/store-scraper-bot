// Upstash Redis adapter.
//
// Logical key namespace:
//   admin                       singleton
//   group:{chatId}              per-group state
//   apple:{appId}               cached Apple response (with TTL)
//   google:{appId}              cached Google response (with TTL)
//
// Multi-tenancy: every physical Redis key carries a configurable prefix
// (env.KEY_PREFIX, default 'store-scraper-bot:') so this bot can safely share
// an Upstash database with other Vercel projects without collision.
//
// 60s minimum TTL clamp is preserved from the KV days for parity safety.

import { Redis } from '@upstash/redis';

const MIN_TTL_SECONDS = 60;
const DEFAULT_KEY_PREFIX = 'store-scraper-bot:';

// Accepts both env var naming conventions:
//   UPSTASH_REDIS_REST_URL / UPSTASH_REDIS_REST_TOKEN (vanilla Upstash signup)
//   KV_REST_API_URL / KV_REST_API_TOKEN (Vercel Marketplace integration)
export function createUpstashClient(env) {
  const url = env?.UPSTASH_REDIS_REST_URL ?? env?.KV_REST_API_URL;
  const token = env?.UPSTASH_REDIS_REST_TOKEN ?? env?.KV_REST_API_TOKEN;
  if (!url) throw new Error('UPSTASH_REDIS_REST_URL or KV_REST_API_URL is required');
  if (!token) throw new Error('UPSTASH_REDIS_REST_TOKEN or KV_REST_API_TOKEN is required');
  const client = new Redis({ url, token });
  const prefix = env.KEY_PREFIX ?? DEFAULT_KEY_PREFIX;
  return { client, prefix };
}

function physicalKey(handle, key) {
  return `${handle.prefix}${key}`;
}

export async function getJson(handle, key) {
  const value = await handle.client.get(physicalKey(handle, key));
  if (value == null) return null;
  // Some SDK versions return strings, others return parsed objects.
  return typeof value === 'string' ? JSON.parse(value) : value;
}

export async function putJson(handle, key, value, opts = {}) {
  const ex =
    opts.expirationTtl != null ? Math.max(MIN_TTL_SECONDS, opts.expirationTtl) : null;
  const setOpts = ex != null ? { ex } : undefined;
  await handle.client.set(physicalKey(handle, key), JSON.stringify(value), setOpts);
}

export async function del(handle, key) {
  await handle.client.del(physicalKey(handle, key));
}
