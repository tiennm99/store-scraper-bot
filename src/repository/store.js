import { createAdminRepository } from './admin-repository.js';
import { createGroupRepository } from './group-repository.js';
import { createAppleAppRepository } from './apple-app-repository.js';
import { createGoogleAppRepository } from './google-app-repository.js';

// Single binding point for all repositories. Threads `env` once so command
// handlers don't need to know about the Worker `env` argument.
export function createStore(env, appCacheSeconds) {
  return {
    admin: createAdminRepository(env),
    group: createGroupRepository(env),
    appleApp: createAppleAppRepository(env, appCacheSeconds),
    googleApp: createGoogleAppRepository(env, appCacheSeconds),
  };
}
