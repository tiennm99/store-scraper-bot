// Upstash Redis adapter — replaces the prior Cloudflare KV wrapper.
//
// Logical key namespace (unchanged from KV layer):
//   admin                       singleton
//   group:{chatId}              per-group state
//   apple:{appId}               cached Apple response (with TTL)
//   google:{appId}              cached Google response (with TTL)
//
// Multi-tenancy: every physical Redis key carries a configurable prefix
// (env.KEY_PREFIX, default 'store-scraper-bot:') so this bot can safely share
// an Upstash database with other Vercel projects without collision. Repository
// callers pass logical keys; the adapter applies the prefix transparently.
//
// 60s minimum TTL clamp is preserved from the KV days for parity safety,
// even though Redis would accept lower values.

import { Redis } from '@upstash/redis';

const MIN_TTL_SECONDS = 60;
const DEFAULT_KEY_PREFIX = 'store-scraper-bot:';

export class UpstashUnavailable extends Error {
  constructor(missing) {
    super(`Upstash env var missing: ${missing}`);
    this.name = 'UpstashUnavailable';
  }
}

// Build a handle bundling the Redis client and the key prefix together.
// The handle is what callers pass into getJson/putJson/del/scan — it stays
// opaque so repositories never need to know about prefixing themselves.
export function createUpstashClient(env) {
  if (!env?.UPSTASH_REDIS_REST_URL) throw new UpstashUnavailable('UPSTASH_REDIS_REST_URL');
  if (!env?.UPSTASH_REDIS_REST_TOKEN) throw new UpstashUnavailable('UPSTASH_REDIS_REST_TOKEN');
  const client = new Redis({
    url: env.UPSTASH_REDIS_REST_URL,
    token: env.UPSTASH_REDIS_REST_TOKEN,
  });
  const prefix = env.KEY_PREFIX ?? DEFAULT_KEY_PREFIX;
  return { client, prefix };
}

function physicalKey(handle, key) {
  return `${handle.prefix}${key}`;
}

// Upstash auto-deserializes values that look like JSON. We always store via
// JSON.stringify, so reads can return the parsed object directly. Returns null
// on missing key, matching the prior KV semantics.
export async function getJson(handle, key) {
  const value = await handle.client.get(physicalKey(handle, key));
  if (value == null) return null;
  // Some SDK versions return strings, others return parsed objects depending
  // on content. Normalize: if string, parse; if object, pass through.
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

// Suffix-based scan. Caller passes a logical match like 'group:*'; adapter
// prepends the key prefix so only this bot's keys are returned.
// Returns the list of *logical* keys (prefix stripped) so callers stay
// prefix-unaware.
export async function scan(handle, matchSuffix) {
  const match = `${handle.prefix}${matchSuffix}`;
  const out = [];
  let cursor = '0';
  do {
    const [next, batch] = await handle.client.scan(cursor, { match, count: 100 });
    cursor = next;
    for (const physical of batch) {
      out.push(physical.startsWith(handle.prefix) ? physical.slice(handle.prefix.length) : physical);
    }
  } while (cursor !== '0');
  return out;
}
