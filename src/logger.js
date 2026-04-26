// Worker-friendly structured logger. Cloudflare Observability indexes JSON
// console output, so we emit one JSON record per call.
export function createLogger() {
  function log(level, payloadOrMsg, maybeMsg) {
    const isObj = payloadOrMsg !== null && typeof payloadOrMsg === 'object';
    const payload = isObj ? payloadOrMsg : {};
    const msg = isObj ? maybeMsg ?? '' : payloadOrMsg ?? '';
    console.log(JSON.stringify({ level, ts: new Date().toISOString(), msg, ...payload }));
  }
  return {
    debug: (p, m) => log('debug', p, m),
    info: (p, m) => log('info', p, m),
    warn: (p, m) => log('warn', p, m),
    error: (p, m) => log('error', p, m),
  };
}
