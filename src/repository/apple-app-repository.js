import { getCollection } from './mongodb.js';
import { isAppleAppExpired } from '../models/apple-app.js';

export function createAppleAppRepository(env, appCacheSeconds) {
  function collection() {
    return getCollection('apple_app', env);
  }

  async function get(appId) {
    const c = await collection();
    return c.findOne({ _id: appId });
  }

  async function save(entry) {
    const c = await collection();
    await c.replaceOne({ _id: entry._id }, entry, { upsert: true });
  }

  async function getCached(appId) {
    const entry = await get(appId);
    if (!entry) return null;
    if (isAppleAppExpired(entry, Date.now(), appCacheSeconds * 1000)) return null;
    return entry;
  }

  return { get, save, getCached };
}
