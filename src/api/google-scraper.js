import gplay from 'google-play-scraper';
import { newGoogleApp } from '../models/google-app.js';

// Mirrors Java GooglePlayScraper (api/google/GooglePlayScraper.java).
// Calls the `google-play-scraper` npm lib directly (no HTTP roundtrip).

export function buildGoogleRequest(appId, country) {
  return { appId, country: country || 'vn' };
}

export function createGoogleScraper(config, repository) {
  const { logger } = config;
  const repo = repository.googleApp;

  async function app(req) {
    return gplay.app(req);
  }

  // rawApp returns a JSON-text representation of the parsed object so the
  // /rawgoogleapp command and any other text consumers stay parity-compatible
  // with the previous HTTP-text response.
  async function rawApp(req) {
    return JSON.stringify(await app(req));
  }

  async function cache(resp, fallbackId) {
    if (!resp) return;
    const id = resp.appId || fallbackId;
    if (!id) return;
    try {
      await repo.save(newGoogleApp(id, resp, Date.now()));
    } catch (err) {
      logger.warn({ appId: id, err: err.message }, 'failed to cache google app');
    }
  }

  async function getApp(appId, country) {
    const cached = await repo.getCached(appId);
    if (cached) return cached.app;
    const resp = await app(buildGoogleRequest(appId, country));
    await cache(resp, appId);
    return resp;
  }

  async function fetchAndCache(req) {
    const resp = await app(req);
    await cache(resp, req.appId);
    return resp;
  }

  return { rawApp, app, getApp, fetchAndCache };
}
