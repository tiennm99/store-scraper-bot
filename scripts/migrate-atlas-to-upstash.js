// One-shot legacy-DB migrator: MongoDB Atlas (java-store-scraper-bot) → Upstash Redis.
// Direct write — no on-disk JSON intermediate.
//
//   common._id="admin"   → SET <prefix>admin <json>
//   group.find({})       → SET <prefix>group:<_id> <json>     (per group)
//   apple_app.find({})   → SET <prefix>apple:<_id> <json> EX <ttl>   (only with --include-cache)
//   google_app.find({})  → SET <prefix>google:<_id> <json> EX <ttl>  (only with --include-cache)
//
// KEY_PREFIX defaults to 'store-scraper-bot:' — must match what the bot
// runtime reads or migrated data is invisible after cutover.
//
// Run:    npm run migrate
// Dry:    npm run migrate:dry
// Cache:  npm run migrate -- --include-cache
//
// Reads .env.deploy for: MONGODB_URI, UPSTASH_REDIS_REST_URL,
// UPSTASH_REDIS_REST_TOKEN, KEY_PREFIX (optional), APP_CACHE_SECONDS (optional).

import { MongoClient } from 'mongodb';
import { Redis } from '@upstash/redis';

const MIN_TTL_SECONDS = 60;
const DEFAULT_KEY_PREFIX = 'store-scraper-bot:';
const APP_CACHE_SECONDS = Number(process.env.APP_CACHE_SECONDS ?? 600);

function exitWith(message) {
  console.error(`migrate-atlas-to-upstash: ${message}`);
  process.exit(1);
}

function log(line) {
  console.log(`migrate-atlas-to-upstash: ${line}`);
}

// Compute remaining TTL in seconds for a cached app entry, given its
// stored `millis` (= cache write time). Returns null if already expired.
function remainingTtl(millis, nowMs) {
  const expiresAt = millis + APP_CACHE_SECONDS * 1000;
  const remainingSec = Math.floor((expiresAt - nowMs) / 1000);
  if (remainingSec <= 0) return null;
  return Math.max(MIN_TTL_SECONDS, remainingSec);
}

async function main() {
  const mongoUri = process.env.MONGODB_URI;
  if (!mongoUri) exitWith('MONGODB_URI not set; check .env.deploy');

  const upstashUrl = process.env.UPSTASH_REDIS_REST_URL;
  const upstashToken = process.env.UPSTASH_REDIS_REST_TOKEN;
  const dryRun = process.argv.includes('--dry-run');
  if (!dryRun) {
    if (!upstashUrl) exitWith('UPSTASH_REDIS_REST_URL not set; check .env.deploy');
    if (!upstashToken) exitWith('UPSTASH_REDIS_REST_TOKEN not set; check .env.deploy');
  }

  const includeCache = process.argv.includes('--include-cache');
  const prefix = process.env.KEY_PREFIX ?? DEFAULT_KEY_PREFIX;

  log(`mode:           ${dryRun ? 'DRY RUN (no writes)' : 'LIVE (writes to Upstash)'}`);
  log(`KEY_PREFIX:     ${prefix}   (must match bot runtime KEY_PREFIX)`);
  log(`include-cache:  ${includeCache}`);

  const redis = dryRun
    ? null
    : new Redis({ url: upstashUrl, token: upstashToken });

  // Long timeout because Atlas free tier auto-pauses idle clusters; first hit
  // can take 10–30 s to wake up. Migration is one-shot, not perf-critical.
  const mongo = new MongoClient(mongoUri, {
    serverSelectionTimeoutMS: 30000,
    socketTimeoutMS: 30000,
    appName: 'migrate-atlas-to-upstash',
  });
  await mongo.connect();
  const db = mongo.db();

  const counts = {
    admin: 0,
    group: 0,
    apple: 0,
    appleSkipped: 0,
    google: 0,
    googleSkipped: 0,
  };

  async function writeKey(logicalKey, value, ttlSeconds = null) {
    const physical = `${prefix}${logicalKey}`;
    if (dryRun) {
      log(`  DRY  SET ${physical}${ttlSeconds != null ? ` EX ${ttlSeconds}` : ''}`);
      return;
    }
    await redis.set(
      physical,
      JSON.stringify(value),
      ttlSeconds != null ? { ex: ttlSeconds } : undefined,
    );
  }

  // 1. admin singleton (common._id = "admin")
  const adminDoc = await db.collection('common').findOne({ _id: 'admin' });
  if (adminDoc) {
    await writeKey('admin', adminDoc);
    counts.admin = 1;
  } else {
    log('warning: no admin doc found in common collection');
  }

  // 2. groups
  const groupDocs = await db.collection('group').find({}).toArray();
  for (const doc of groupDocs) {
    await writeKey(`group:${doc._id}`, doc);
  }
  counts.group = groupDocs.length;

  // 3. caches (opt-in)
  if (includeCache) {
    const now = Date.now();
    const appleDocs = await db.collection('apple_app').find({}).toArray();
    for (const doc of appleDocs) {
      const ttl = remainingTtl(doc.millis ?? 0, now);
      if (ttl == null) {
        counts.appleSkipped++;
        continue;
      }
      await writeKey(`apple:${doc._id}`, doc, ttl);
      counts.apple++;
    }

    const googleDocs = await db.collection('google_app').find({}).toArray();
    for (const doc of googleDocs) {
      const ttl = remainingTtl(doc.millis ?? 0, now);
      if (ttl == null) {
        counts.googleSkipped++;
        continue;
      }
      await writeKey(`google:${doc._id}`, doc, ttl);
      counts.google++;
    }
  }

  await mongo.close();

  log('---');
  log(`admin:   ${counts.admin}`);
  log(`groups:  ${counts.group}`);
  if (includeCache) {
    log(`apple:   ${counts.apple} (skipped ${counts.appleSkipped} expired)`);
    log(`google:  ${counts.google} (skipped ${counts.googleSkipped} expired)`);
  } else {
    log('caches:  skipped (use --include-cache to migrate them)');
  }
  log('');
  log(dryRun ? 'dry run complete — no Upstash writes performed' : 'migration complete');
}

main().catch((err) => exitWith(err.stack ?? err.message ?? String(err)));
