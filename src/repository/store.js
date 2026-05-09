import { createAdminRepository } from './admin-repository.js';
import { createGroupRepository } from './group-repository.js';
import { createAppleAppRepository } from './apple-app-repository.js';
import { createGoogleAppRepository } from './google-app-repository.js';

// Single binding point for all repositories. Threads the Upstash handle
// (client + key prefix) once so command handlers don't need to know about
// process.env or the Redis client construction.
export function createStore(handle, appCacheSeconds) {
  return {
    admin: createAdminRepository(handle),
    group: createGroupRepository(handle),
    appleApp: createAppleAppRepository(handle, appCacheSeconds),
    googleApp: createGoogleAppRepository(handle, appCacheSeconds),
  };
}
