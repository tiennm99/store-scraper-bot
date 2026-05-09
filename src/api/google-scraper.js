import gplay from 'google-play-scraper';

// Calls the `google-play-scraper` npm lib directly (no HTTP roundtrip).

export function buildGoogleRequest(appId, country) {
  return { appId, country: country || 'vn' };
}

export function createGoogleScraper(config, cache) {
  const { logger } = config;

  async function rawApp(req) {
    return JSON.stringify(await gplay.app(req));
  }

  async function saveCache(resp, fallbackId) {
    if (!resp) return;
    const id = resp.appId || fallbackId;
    if (!id) return;
    try {
      await cache.save(id, resp);
    } catch (err) {
      logger.warn({ appId: id, err: err.message }, 'failed to cache google app');
    }
  }

  async function getApp(appId, country) {
    const cached = await cache.getCached(appId);
    if (cached) return cached.app;
    const resp = await gplay.app(buildGoogleRequest(appId, country));
    await saveCache(resp, appId);
    return resp;
  }

  async function fetchAndCache(req) {
    const resp = await gplay.app(req);
    await saveCache(resp, req.appId);
    return resp;
  }

  return { rawApp, getApp, fetchAndCache };
}
