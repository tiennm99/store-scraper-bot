// AppleApp cache entry — Java parity (_id=appId, class="AppleApp").
// TTL is enforced by Cloudflare KV via expirationTtl, so no isExpired helper.
export function newAppleApp(appId, response, millis) {
  return { _id: appId, class: 'AppleApp', app: response, millis };
}
