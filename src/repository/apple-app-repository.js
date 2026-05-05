import { getJson, putJson } from './kv.js';

// KV-backed Apple app cache. Key shape: `apple:{appId}`.
// KV's expirationTtl replaces Java/Mongo's manual `(now - millis) > cacheMillis`
// check — expired keys are deleted, so a get() returning null is the cache miss.
export function createAppleAppRepository(env, appCacheSeconds) {
  function key(appId) {
    return `apple:${appId}`;
  }

  async function get(appId) {
    return getJson(env, key(appId));
  }

  async function save(entry) {
    await putJson(env, key(entry._id), entry, { expirationTtl: appCacheSeconds });
  }

  async function getCached(appId) {
    return get(appId);
  }

  return { get, save, getCached };
}
