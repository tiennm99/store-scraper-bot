import { getCollection } from './mongodb.js';
import {
  ADMIN_ID,
  adminAddGroup,
  adminHasGroup,
  adminRemoveGroup,
  newAdmin,
} from '../models/admin.js';

// Stored in "common" collection at _id="admin" (Java parity).
export function createAdminRepository(env) {
  function collection() {
    return getCollection('common', env);
  }

  async function init() {
    const c = await collection();
    const count = await c.countDocuments({ _id: ADMIN_ID });
    if (count > 0) return;
    await save(newAdmin());
  }

  async function getAdmin() {
    const c = await collection();
    const doc = await c.findOne({ _id: ADMIN_ID });
    return doc ?? newAdmin();
  }

  async function save(admin) {
    const c = await collection();
    await c.replaceOne({ _id: ADMIN_ID }, admin, { upsert: true });
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
