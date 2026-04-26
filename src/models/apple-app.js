// AppleApp cache entry — Java parity (_id=appId, class="AppleApp").
export function newAppleApp(appId, response, millis) {
  return { _id: appId, class: 'AppleApp', app: response, millis };
}

export function isAppleAppExpired(entry, nowMillis, cacheMillis) {
  return nowMillis - entry.millis > cacheMillis;
}
