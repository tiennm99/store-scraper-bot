#!/usr/bin/env node
// One-shot inspector: lists every Redis key the bot can see.
// Run via: node --env-file=.env.deploy scripts/list-upstash-keys.js
// Read-only.

import { Redis } from '@upstash/redis';

const url = process.env.UPSTASH_REDIS_REST_URL ?? process.env.KV_REST_API_URL;
const token = process.env.UPSTASH_REDIS_REST_TOKEN ?? process.env.KV_REST_API_TOKEN;
const prefix = process.env.KEY_PREFIX ?? 'store-scraper-bot:';

if (!url || !token) {
  console.error('Upstash credentials not found in env');
  process.exit(1);
}

const redis = new Redis({ url, token });

let cursor = 0;
const keys = [];
do {
  const [next, batch] = await redis.scan(cursor, { match: '*', count: 100 });
  cursor = Number(next);
  keys.push(...batch);
} while (cursor !== 0);

keys.sort();

const inPrefix = keys.filter((k) => k.startsWith(prefix));
const orphan = keys.filter((k) => !k.startsWith(prefix));

console.log(`KEY_PREFIX: ${prefix}`);
console.log(`Total keys: ${keys.length}`);
console.log(`In-prefix:  ${inPrefix.length}`);
console.log(`Orphan:     ${orphan.length}`);
console.log();
console.log('In-prefix keys:');
for (const k of inPrefix) console.log(`  ${k}`);
if (orphan.length) {
  console.log();
  console.log('Orphan keys (NOT under KEY_PREFIX):');
  for (const k of orphan) console.log(`  ${k}`);
}
