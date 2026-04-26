import { getCollection } from './mongodb.js';
import { isGoogleAppExpired } from '../models/google-app.js';

function collection() {
  return getCollection('google_app');
}

export async function getGoogleApp(appId) {
  return collection().findOne({ _id: appId });
}

export async function saveGoogleApp(entry) {
  await collection().replaceOne({ _id: entry._id }, entry, { upsert: true });
}

export async function getCachedGoogleApp(appId, appCacheSeconds) {
  const entry = await getGoogleApp(appId);
  if (!entry) return null;
  const cacheMillis = appCacheSeconds * 1000;
  if (isGoogleAppExpired(entry, Date.now(), cacheMillis)) return null;
  return entry;
}
