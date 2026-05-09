import { getJson, putJson } from './upstash.js';

const ADMIN_KEY = 'admin';

// Upstash-backed admin singleton. Holds the authorized chat ID allowlist
// plus optional admin-global runtime settings (e.g. cache TTL override).
export function createAdminRepository(handle) {
  async function load() {
    return (await getJson(handle, ADMIN_KEY)) ?? { groups: [] };
  }

  async function save(admin) {
    await putJson(handle, ADMIN_KEY, admin);
  }

  async function addGroup(groupId) {
    const admin = await load();
    if (admin.groups.includes(groupId)) return false;
    admin.groups.push(groupId);
    await save(admin);
    return true;
  }

  async function removeGroup(groupId) {
    const admin = await load();
    const i = admin.groups.indexOf(groupId);
    if (i < 0) return false;
    admin.groups.splice(i, 1);
    await save(admin);
    return true;
  }

  async function hasGroup(groupId) {
    const admin = await load();
    return admin.groups.includes(groupId);
  }

  async function getAllGroups() {
    return (await load()).groups;
  }

  // undefined when no override set — caller falls back to env default.
  async function getAppCacheSeconds() {
    return (await load()).appCacheSeconds;
  }

  // Pass `undefined` to clear the override.
  async function setAppCacheSeconds(seconds) {
    const admin = await load();
    if (seconds === undefined) delete admin.appCacheSeconds;
    else admin.appCacheSeconds = seconds;
    await save(admin);
  }

  return {
    addGroup,
    removeGroup,
    hasGroup,
    getAllGroups,
    getAppCacheSeconds,
    setAppCacheSeconds,
  };
}
