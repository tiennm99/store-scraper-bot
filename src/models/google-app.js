// GoogleApp cache entry — Java parity (_id=appId, class="GoogleApp").
// TTL is enforced by Cloudflare KV via expirationTtl, so no isExpired helper.
export function newGoogleApp(appId, response, millis) {
  return { _id: appId, class: 'GoogleApp', app: response, millis };
}
