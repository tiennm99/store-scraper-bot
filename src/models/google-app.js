// GoogleApp cache entry — Java parity (_id=appId, class="GoogleApp").
export function newGoogleApp(appId, response, millis) {
  return { _id: appId, class: 'GoogleApp', app: response, millis };
}

export function isGoogleAppExpired(entry, nowMillis, cacheMillis) {
  return nowMillis - entry.millis > cacheMillis;
}
