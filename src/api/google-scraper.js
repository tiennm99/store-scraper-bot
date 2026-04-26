import { getCachedGoogleApp, saveGoogleApp } from '../repository/google-app-repository.js';
import { newGoogleApp } from '../models/google-app.js';

// Mirrors Java GooglePlayScraper (api/google/GooglePlayScraper.java).
const BASE_URL = 'https://store-scraper.vercel.app/google';

export function buildGoogleRequest(appId, country) {
  return { appId, country: country || 'vn' };
}

export function createGoogleScraper(config) {
  const { logger, appCacheSeconds } = config;

  async function rawApp(req) {
    const res = await fetch(`${BASE_URL}/app`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) throw new Error(`google HTTP status ${res.status}`);
    return await res.text();
  }

  async function app(req) {
    const text = await rawApp(req);
    return JSON.parse(text);
  }

  async function cache(resp, fallbackId) {
    if (!resp) return;
    const id = resp.appId || fallbackId;
    if (!id) return;
    try {
      await saveGoogleApp(newGoogleApp(id, resp, Date.now()));
    } catch (err) {
      logger.warn({ appId: id, err: err.message }, 'failed to cache google app');
    }
  }

  async function getApp(appId, country) {
    const cached = await getCachedGoogleApp(appId, appCacheSeconds);
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
