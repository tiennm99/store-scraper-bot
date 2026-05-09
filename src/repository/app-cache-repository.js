import { getJson, putJson } from './upstash.js';

// Per-store cache for upstream app responses. TTL via Redis EX —
// expired keys are deleted, so a null read is the cache miss.
// `prefix` is the logical key namespace ('apple' or 'google').
export function createAppCacheRepository(handle, prefix, appCacheSeconds) {
  function key(appId) {
    return `${prefix}:${appId}`;
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
        { expirationTtl: appCacheSeconds },
      );
    },
  };
}
