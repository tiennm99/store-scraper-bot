import store from 'app-store-scraper';
import { newAppleApp } from '../models/apple-app.js';

// Mirrors Java AppStoreScraper (api/apple/AppStoreScraper.java).
// Calls the `app-store-scraper` npm lib directly (no HTTP roundtrip).

export function buildAppleRequestByTrackId(id, country) {
  return { id, country, ratings: true };
}

export function buildAppleRequestByBundleId(appId, country) {
  return { appId, country, ratings: true };
}

export function createAppleScraper(config, repository) {
  const { logger } = config;
  const repo = repository.appleApp;

  async function app(req) {
    return store.app(req);
  }

  // rawApp returns a JSON-text representation of the parsed object so the
  // /rawappleapp command and any other text consumers stay parity-compatible
  // with the previous HTTP-text response.
  async function rawApp(req) {
    return JSON.stringify(await app(req));
  }

  async function cache(resp) {
    if (!resp || !resp.appId) return;
    try {
      await repo.save(newAppleApp(resp.appId, resp, Date.now()));
    } catch (err) {
      logger.warn({ appId: resp.appId, err: err.message }, 'failed to cache apple app');
    }
  }

  async function getApp(appId, country) {
    const cached = await repo.getCached(appId);
    if (cached) return cached.app;
    const resp = await app(buildAppleRequestByBundleId(appId, country));
    await cache(resp);
    return resp;
  }

  async function fetchAndCache(req) {
    const resp = await app(req);
    await cache(resp);
    return resp;
  }

  return { rawApp, app, getApp, fetchAndCache };
}
