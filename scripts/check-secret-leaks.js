#!/usr/bin/env node
// Fails CI if any source file logs a secret via env.
// Pattern: console.{log,info,warn,error,debug}(... env.<SECRET> ...)
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

const SECRETS = ['MONGODB_URI', 'TELEGRAM_BOT_TOKEN', 'TELEGRAM_WEBHOOK_SECRET', 'ADMIN_IDS'];
const ROOTS = ['src', 'scripts'];

function* walk(dir) {
  let entries;
  try {
    entries = readdirSync(dir);
  } catch {
    return;
  }
  for (const entry of entries) {
    const p = join(dir, entry);
    const s = statSync(p);
    if (s.isDirectory()) yield* walk(p);
    else if (/\.(js|mjs|ts)$/.test(p)) yield p;
  }
}

const violations = [];
for (const root of ROOTS) {
  for (const file of walk(root)) {
    const text = readFileSync(file, 'utf8');
    for (const secret of SECRETS) {
      const re = new RegExp(`console\\.(log|info|warn|error|debug)\\([^)]*\\benv\\.${secret}\\b`);
      if (re.test(text)) violations.push({ file, secret });
    }
  }
}

if (violations.length > 0) {
  console.error('Secret-leak violations:');
  for (const v of violations) console.error(`  ${v.file}: env.${v.secret} in console.*`);
  process.exit(1);
}
console.log('check-secret-leaks: clean');
