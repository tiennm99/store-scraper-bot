import { getJson, putJson } from './upstash.js';
import {
  ADMIN_ID,
  adminAddGroup,
  adminHasGroup,
  adminRemoveGroup,
  newAdmin,
} from '../models/admin.js';

// Upstash-backed admin singleton — Java parity at the document level
// (logical key 'admin' holds the same shape Mongo stored at _id="admin").
// The physical Redis key carries the configured KEY_PREFIX (handled by adapter).
export function createAdminRepository(handle) {
  async function init() {
    const existing = await getJson(handle, ADMIN_ID);
    if (existing) return;
    await save(newAdmin());
  }

  async function getAdmin() {
    const doc = await getJson(handle, ADMIN_ID);
    return doc ?? newAdmin();
  }

  async function save(admin) {
    await putJson(handle, ADMIN_ID, admin);
  }

  async function addGroup(groupId) {
    const admin = await getAdmin();
    if (!adminAddGroup(admin, groupId)) return false;
    await save(admin);
    return true;
  }

  async function removeGroup(groupId) {
    const admin = await getAdmin();
    if (!adminRemoveGroup(admin, groupId)) return false;
    await save(admin);
    return true;
  }

  async function hasGroup(groupId) {
    const admin = await getAdmin();
    return adminHasGroup(admin, groupId);
  }

  async function getAllGroups() {
    const admin = await getAdmin();
    return admin.groups;
  }

  return { init, getAdmin, save, addGroup, removeGroup, hasGroup, getAllGroups };
}
