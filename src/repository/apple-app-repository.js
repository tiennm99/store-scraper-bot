import { getCollection } from './mongodb.js';
import { isAppleAppExpired } from '../models/apple-app.js';

function collection() {
  return getCollection('apple_app');
}

export async function getAppleApp(appId) {
  return collection().findOne({ _id: appId });
}

export async function saveAppleApp(entry) {
  await collection().replaceOne({ _id: entry._id }, entry, { upsert: true });
}

export async function getCachedAppleApp(appId, appCacheSeconds) {
  const entry = await getAppleApp(appId);
  if (!entry) return null;
  const cacheMillis = appCacheSeconds * 1000;
  if (isAppleAppExpired(entry, Date.now(), cacheMillis)) return null;
  return entry;
}
