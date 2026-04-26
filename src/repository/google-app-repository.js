import { getCollection } from './mongodb.js';
import { isGoogleAppExpired } from '../models/google-app.js';

export function createGoogleAppRepository(env, appCacheSeconds) {
  function collection() {
    return getCollection('google_app', env);
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
    if (isGoogleAppExpired(entry, Date.now(), appCacheSeconds * 1000)) return null;
    return entry;
  }

  return { get, save, getCached };
}
