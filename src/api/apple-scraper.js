import store from 'app-store-scraper';

// Calls the `app-store-scraper` npm lib directly (no HTTP roundtrip).

export function buildAppleRequestByTrackId(id, country) {
  return { id, country, ratings: true };
}

export function buildAppleRequestByBundleId(appId, country) {
  return { appId, country, ratings: true };
}

export function createAppleScraper(config, cache) {
  const { logger } = config;

  async function rawApp(req) {
    return JSON.stringify(await store.app(req));
  }

  async function saveCache(resp) {
    if (!resp || !resp.appId) return;
    try {
      await cache.save(resp.appId, resp);
    } catch (err) {
      logger.warn({ appId: resp.appId, err: err.message }, 'failed to cache apple app');
    }
  }

  async function getApp(appId, country) {
    const cached = await cache.getCached(appId);
    if (cached) return cached.app;
    const resp = await store.app(buildAppleRequestByBundleId(appId, country));
    await saveCache(resp);
    return resp;
  }

  async function fetchAndCache(req) {
    const resp = await store.app(req);
    await saveCache(resp);
    return resp;
  }

  return { rawApp, getApp, fetchAndCache };
}
