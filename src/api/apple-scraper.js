import { newAppleApp } from '../models/apple-app.js';

// Mirrors Java AppStoreScraper (api/apple/AppStoreScraper.java).
const BASE_URL = 'https://store-scraper.vercel.app/apple';

export function buildAppleRequestByTrackId(id, country) {
  return { id, country, ratings: true };
}

export function buildAppleRequestByBundleId(appId, country) {
  return { appId, country, ratings: true };
}

export function createAppleScraper(config, store) {
  const { logger } = config;
  const repo = store.appleApp;

  async function rawApp(req) {
    const res = await fetch(`${BASE_URL}/app`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error(`apple HTTP status ${res.status}`);
    return await res.text();
  }

  async function app(req) {
    const text = await rawApp(req);
    return JSON.parse(text);
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
