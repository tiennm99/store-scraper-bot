import { MongoClient } from 'mongodb';

// Thrown when the driver fails to reach Atlas (e.g. paused cluster, network).
// Command handlers catch this and reply with "Internal server error".
export class MongoUnavailable extends Error {
  constructor(cause) {
    super(`MongoDB unavailable: ${cause.message}`);
    this.name = 'MongoUnavailable';
    this.cause = cause;
  }
}

// Memoized per warm Worker isolate. Module-scope is per-isolate in Workers,
// so this caches one Promise<{client, db}> for the isolate's lifetime.
let memoized = null;

export async function getMongo(env) {
  if (memoized) return memoized;
  memoized = (async () => {
    try {
      const client = new MongoClient(env.MONGODB_URI, {
        serverSelectionTimeoutMS: 5000,
        socketTimeoutMS: 10000,
        appName: 'js-store-scraper-bot',
      });
      await client.connect();
      // db() with no arg uses the database from the URI path.
      const db = client.db();
      return { client, db };
    } catch (err) {
      memoized = null; // allow retry on next request
      throw new MongoUnavailable(err);
    }
  })();
  return memoized;
}

export async function getCollection(name, env) {
  const { db } = await getMongo(env);
  return db.collection(name);
}
