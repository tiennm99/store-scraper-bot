import { getJson, putJson } from './upstash.js';

// Upstash-backed Google app cache. Logical key shape: `google:{appId}`.
// Redis EX (via expirationTtl) replaces Java/Mongo's manual
// `(now - millis) > cacheMillis` check — expired keys are deleted, so a
// get() returning null is the cache miss.
export function createGoogleAppRepository(handle, appCacheSeconds) {
  function key(appId) {
    return `google:${appId}`;
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
