// Thin wrapper around the Cloudflare KV binding `env.STORE_KV`.
// All four logical collections live in one namespace, separated by key prefix:
//   admin                       singleton
//   group:{chatId}              per-group state
//   apple:{appId}               cached Apple response (with KV TTL)
//   google:{appId}              cached Google response (with KV TTL)

// KV's minimum expirationTtl is 60s. Java/Mongo had no such floor; clamp here
// so a low APP_CACHE_SECONDS override doesn't make put() reject.
const KV_MIN_TTL_SECONDS = 60;

export class KvUnavailable extends Error {
  constructor() {
    super('STORE_KV binding is missing — check wrangler.toml [[kv_namespaces]]');
    this.name = 'KvUnavailable';
  }
}

function binding(env) {
  if (!env || !env.STORE_KV) throw new KvUnavailable();
  return env.STORE_KV;
}

export async function getJson(env, key) {
  return binding(env).get(key, 'json');
}

export async function putJson(env, key, value, opts = {}) {
  const putOpts = { ...opts };
  if (putOpts.expirationTtl != null) {
    putOpts.expirationTtl = Math.max(KV_MIN_TTL_SECONDS, putOpts.expirationTtl);
  }
  await binding(env).put(key, JSON.stringify(value), putOpts);
}

export async function del(env, key) {
  await binding(env).delete(key);
}
