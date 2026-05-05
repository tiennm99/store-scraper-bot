// One-shot Atlas → Cloudflare KV exporter.
// Reads `common` (admin singleton) and `group` collections from MongoDB and
// writes a bulk-put JSON file consumable by:
//
//   wrangler kv bulk put --binding STORE_KV --remote scripts/.atlas-export.json
//
// Cache collections (apple_app, google_app) are skipped by default — they
// auto-rebuild from upstream within `APP_CACHE_SECONDS`. Pass --include-cache
// to migrate them with their TTL recomputed (already-expired entries skipped).
//
// Run: npm run migrate            # then: npm run migrate:bulk
//
// Requires .env with MONGODB_URI; loaded by package.json's `node --env-file=.env`.

import { writeFile } from 'node:fs/promises';
import { MongoClient } from 'mongodb';

const OUT_PATH = 'scripts/.atlas-export.json';
const KV_MIN_TTL_SECONDS = 60;
const APP_CACHE_SECONDS = Number(process.env.APP_CACHE_SECONDS ?? 600);

function exitWith(message) {
  console.error(`migrate-atlas-to-kv: ${message}`);
  process.exit(1);
}

function log(line) {
  console.log(`migrate-atlas-to-kv: ${line}`);
}

// Compute remaining TTL in seconds for a cached app entry, given its
// stored `millis` (= cache write time). Returns null if already expired.
function remainingTtl(millis, nowMs) {
  const expiresAt = millis + APP_CACHE_SECONDS * 1000;
  const remainingSec = Math.floor((expiresAt - nowMs) / 1000);
  if (remainingSec <= 0) return null;
  return Math.max(KV_MIN_TTL_SECONDS, remainingSec);
}

async function main() {
  const uri = process.env.MONGODB_URI;
  if (!uri) exitWith('MONGODB_URI not set; check .env');

  const includeCache = process.argv.includes('--include-cache');

  const client = new MongoClient(uri, {
    serverSelectionTimeoutMS: 5000,
    socketTimeoutMS: 10000,
    appName: 'migrate-atlas-to-kv',
  });
  await client.connect();
  const db = client.db();

  const entries = [];
  const counts = { admin: 0, group: 0, apple: 0, appleSkipped: 0, google: 0, googleSkipped: 0 };

  const adminDoc = await db.collection('common').findOne({ _id: 'admin' });
  if (adminDoc) {
    entries.push({ key: 'admin', value: JSON.stringify(adminDoc) });
    counts.admin = 1;
  } else {
    log('warning: no admin doc found in common collection');
  }

  const groupDocs = await db.collection('group').find({}).toArray();
  for (const doc of groupDocs) {
    entries.push({ key: `group:${doc._id}`, value: JSON.stringify(doc) });
  }
  counts.group = groupDocs.length;

  if (includeCache) {
    const now = Date.now();
    const appleDocs = await db.collection('apple_app').find({}).toArray();
    for (const doc of appleDocs) {
      const ttl = remainingTtl(doc.millis ?? 0, now);
      if (ttl == null) {
        counts.appleSkipped++;
        continue;
      }
      entries.push({ key: `apple:${doc._id}`, value: JSON.stringify(doc), expiration_ttl: ttl });
      counts.apple++;
    }

    const googleDocs = await db.collection('google_app').find({}).toArray();
    for (const doc of googleDocs) {
      const ttl = remainingTtl(doc.millis ?? 0, now);
      if (ttl == null) {
        counts.googleSkipped++;
        continue;
      }
      entries.push({ key: `google:${doc._id}`, value: JSON.stringify(doc), expiration_ttl: ttl });
      counts.google++;
    }
  }

  await client.close();

  if (entries.length > 10000) {
    exitWith(`bulk put limit is 10000; got ${entries.length}. Chunk the export manually.`);
  }

  await writeFile(OUT_PATH, JSON.stringify(entries, null, 2));

  log(`wrote ${entries.length} entries to ${OUT_PATH}`);
  log(`  admin: ${counts.admin}`);
  log(`  groups: ${counts.group}`);
  if (includeCache) {
    log(`  apple: ${counts.apple} (skipped ${counts.appleSkipped} expired)`);
    log(`  google: ${counts.google} (skipped ${counts.googleSkipped} expired)`);
  } else {
    log('  caches: skipped (use --include-cache to migrate them)');
  }
  log('');
  log('next:  npm run migrate:bulk');
  log('then:  rm scripts/.atlas-export.json   (contains your data)');
}

main().catch((err) => exitWith(err.stack ?? err.message ?? String(err)));
