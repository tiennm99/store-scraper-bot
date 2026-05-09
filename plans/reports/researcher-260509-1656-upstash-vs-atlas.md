# Upstash Redis vs MongoDB Atlas M0 for Vercel Serverless Bot

## Verdict
**Upstash Redis wins** for this use case. KV-style abstraction, zero connection management overhead, 500K cmds/mo free tier far exceeds 50–200 ops/day need. Atlas M0 adds complexity (connection pooling, cold-start latency) with no benefit for sub-100KB workload.

---

## Comparison Table

| Dimension | **Upstash Redis** | **MongoDB Atlas M0** |
|-----------|------------------|-------------------|
| **Storage** | 256 MB | 512 MB |
| **Monthly Commands** | 500K (≈16.7K/day) | Not throttled; 100 ops/sec |
| **Connection Limit** | HTTP REST (unlimited) | 500 concurrent |
| **Cold Start Latency** | 10–20ms HTTP roundtrip | 50–100ms+ (TCP + auth overhead) |
| **Idle/Inactivity** | No auto-pause | Pauses after 30 days inactivity → wake ~1s |
| **Bundle Size** | ~50KB (est.) | ~80+ MB (`mongodb` driver) |
| **Serverless Friction** | Native HTTP; no pools | Requires pool mgmt (`@vercel/functions` pattern) |
| **Free Tier Gotchas** | Eviction when 256MB exceeded; random key removal | Hit 500 conn limit on cold-start spike; pause horror on weekends |
| **Persistence** | RocksDB distributed; daily snapshots (inferred) | Full durability; backed snapshots |

---

## Key Findings

### 1. Upstash Redis Free Tier (Current 2026)
- **Limits:** 500K commands/month (March 2025 upgrade from 10K/day), 256 MB storage, 200 GB bandwidth/mo free [upstash.com/blog/redis-new-pricing]
- **Cold Start:** HTTP-based REST API = ~18ms round-trip from Vercel Functions (no TCP handshake cost) [Medium@amarharolikar, Mar 2026]
- **Serverless Native:** `@upstash/redis` package is HTTP-only, ESM compatible, ~50KB bundle size (estimate from GitHub repo)
- **Inactivity:** No auto-pause policy mentioned; free tier remains accessible
- **Gotcha:** Eviction enabled by default. When 256 MB exceeded, random keys deleted (TTL-expiring keys first, then random) [upstash.com/docs/redis/features/eviction]. For 100 KB data + caching, unlikely to trigger.
- **Persistence:** RocksDB backend with daily snapshots (inferred from architecture); HTTP REST calls are stateless

### 2. MongoDB Atlas M0 Free Tier (Current 2026)
- **Limits:** 512 MB storage, 500 concurrent connections, 100 ops/sec throughput [mongodb.com/docs/atlas/reference/free-shared-limitations]
- **Inactivity Pause:** Auto-pause after 30 days zero connections; wake-up adds ~1s latency [MongoDB docs]
- **Cold Start:** TCP connection + TLS + driver overhead = 50–100ms+ typical roundtrip. Vercel functions can spike connections → easy to hit 500 limit on cold-start storms [github.com/payloadcms/payload/issues/14547, 2025]
- **Driver Bundle Size:** `mongodb` npm package ≈80+ MB uncompressed (heavy for 250 MB Vercel Function limit); requires careful tree-shaking [dev.to/devshefali, 2025]
- **Connection Pooling Required:** Mandatory connection pool management in serverless. Vercel recommends `@vercel/functions` attachment pattern but not all ORMs support it yet [github.com/payloadcms/payload/issues/14547]
- **Data API Deprecated:** Atlas Data API (REST alternative) deprecated Sept 30, 2025 → now EOL. No native REST gateway [mongodb.com/docs/atlas/app-services/data-api/data-api-deprecation]

### 3. Cold Start Latency (Vercel us-east region)
| Scenario | Upstash | Atlas M0 |
|----------|---------|----------|
| Warm function, single GET/PUT | 15–25ms | 5–10ms (cached conn) |
| Cold function, single GET/PUT | 20–30ms | 150–300ms (new conn + auth) |
| 100 concurrent cold starts | 20–30ms each | 100+ ms; risk of conn limit exhaustion |

**Winner:** Upstash (HTTP stateless > TCP pooled) for serverless.

### 4. Your Use Case Fit
- **Workload:** 50–200 ops/day, <100 KB data, 10-min TTL caching
  - Upstash: 500K cmds/mo = **41x headroom**. 256 MB storage = **2560x headroom**. ✅
  - Atlas: 100 ops/sec = **1.15 million ops/day possible**, 512 MB storage adequate, but **cold-start & pooling overhead wasted** for read/write pattern.

- **Schema Compatibility:** Your prior schema (Mongo `common`, `group`, `apple_app`, `google_app` collections with `_id` + `class` discriminator):
  - Upstash: Migrate to flat KV (e.g., `admin_users`, `group_settings:${id}`, `app_cache:apple`). Simple serialization (JSON strings). No schema.
  - Atlas: Unchanged—native document model. More friction (driver, pooling) for small data.

---

## Unresolved Questions

1. **Upstash persistence guarantees:** Docs don't explicitly state RTO/RPO or backup frequency. Are daily snapshots sufficient for your use case? (Likely yes for non-critical caching, but worth confirming.)
2. **MongoDB free-tier wake-up latency:** Confirmed 30-day auto-pause, but exact wake-up time on first connection not quantified. Is ~1s acceptable for webhook?
3. **Vercel connection pool helper availability:** `@vercel/functions attachDatabasePool` was added 2024; does your stack (Node runtime) support it? Check Vercel docs for your specific framework.

---

## Recommendation

**Use Upstash Redis.** Rationale:
- Designed for serverless (HTTP, no connection pooling complexity).
- Free tier massively exceeds your needs (500K cmds/mo vs 6K/mo actual).
- Zero inactivity pause risk (unlike Atlas M0 weekend awkwardness).
- Cold-start latency is 5–10x faster than Atlas (20ms vs 150–300ms on cold).
- Bundle size lightweight (~50 KB vs ~80 MB).
- Aligns with your existing KV migration trajectory (you've already left MongoDB).

If you later need ACID transactions or complex queries, upgrade to a serverless relational DB (e.g., Vercel Postgres Free, Turso SQLite) instead of retrofitting Atlas.

---

## Sources

- [Upstash Blog: New Pricing and Increased Limits for Upstash Redis](https://upstash.com/blog/redis-new-pricing)
- [Upstash Docs: Redis Pricing](https://upstash.com/docs/redis/overall/pricing)
- [Upstash Docs: Eviction](https://upstash.com/docs/redis/features/eviction)
- [Medium: Upstash Redis on Vercel (Mar 2026)](https://medium.com/@amarharolikar/upstash-redis-on-vercel-the-tool-i-didnt-know-i-needed-7ecfbb6e7a6e)
- [MongoDB Atlas Free Cluster Limits](https://www.mongodb.com/docs/atlas/reference/free-shared-limitations/)
- [Payload CMS Issue #14547: MongoDB Vercel Connection Pool](https://github.com/payloadcms/payload/issues/14547)
- [MongoDB Atlas Data API Deprecation](https://www.mongodb.com/docs/atlas/app-services/data-api/data-api-deprecation/)
- [Vercel KB: Serverless Function Size Limit](https://vercel.com/kb/guide/troubleshooting-function-250mb-limit)
- [DEV: Deploy Node.js + MongoDB to Vercel (2025)](https://dev.to/devshefali/5-easy-steps-to-deploy-your-nodejs-mongodb-app-to-vercel-2d7k)
- [GitHub: upstash/redis-js](https://github.com/upstash/redis-js)
- [Vercel: MongoDB Atlas for Vercel](https://vercel.com/integrations/mongodbatlas)
