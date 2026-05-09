import { getJson, putJson } from './upstash.js';

// Upstash-backed Apple app cache. Logical key shape: `apple:{appId}`.
// Redis EX (via expirationTtl) replaces Java/Mongo's manual
// `(now - millis) > cacheMillis` check — expired keys are deleted, so a
// get() returning null is the cache miss.
export function createAppleAppRepository(handle, appCacheSeconds) {
  function key(appId) {
    return `apple:${appId}`;
  }

  async function get(appId) {
    return getJson(handle, key(appId));
  }

  async function save(entry) {
    await putJson(handle, key(entry._id), entry, { expirationTtl: appCacheSeconds });
  }

  async function getCached(appId) {
    return get(appId);
  }

  return { get, save, getCached };
}
