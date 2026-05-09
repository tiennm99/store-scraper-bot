import { getJson, putJson } from './upstash.js';

// Per-store cache for upstream app responses. TTL via Redis EX —
// expired keys are deleted, so a null read is the cache miss.
// `prefix` is the logical key namespace ('apple' or 'google').
// `getTtl` is an async function returning the current TTL in seconds; the
// resolved value is memoized for the lifetime of this repo instance, so the
// admin override doc is read at most once per request even with multiple
// cache writes.
export function createAppCacheRepository(handle, prefix, getTtl) {
  function key(appId) {
    return `${prefix}:${appId}`;
  }

  let cachedTtl;
  async function ttl() {
    if (cachedTtl === undefined) cachedTtl = await getTtl();
    return cachedTtl;
  }

  return {
    async getCached(appId) {
      return getJson(handle, key(appId));
    },
    async save(appId, response) {
      await putJson(
        handle,
        key(appId),
        { app: response, millis: Date.now() },
        { expirationTtl: await ttl() },
      );
    },
  };
}
