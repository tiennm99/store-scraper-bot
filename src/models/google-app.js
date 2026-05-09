// GoogleApp cache entry — Java parity (_id=appId, class="GoogleApp").
// TTL is enforced by Upstash Redis EX, so no isExpired helper.
export function newGoogleApp(appId, response, millis) {
  return { _id: appId, class: 'GoogleApp', app: response, millis };
}
