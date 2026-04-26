import { getCollection } from './mongodb.js';
import { ADMIN_ID, adminAddGroup, adminHasGroup, adminRemoveGroup, newAdmin } from '../models/admin.js';

// Stored in "common" collection at _id="admin" (Java parity).
function collection() {
  return getCollection('common');
}

export async function initAdmin() {
  const c = collection();
  const count = await c.countDocuments({ _id: ADMIN_ID });
  if (count > 0) return;
  await save(newAdmin());
}

export async function getAdmin() {
  const doc = await collection().findOne({ _id: ADMIN_ID });
  return doc ?? newAdmin();
}

export async function save(admin) {
  await collection().replaceOne({ _id: ADMIN_ID }, admin, { upsert: true });
}

export async function addGroup(groupId) {
  const admin = await getAdmin();
  if (!adminAddGroup(admin, groupId)) return false;
  await save(admin);
  return true;
}

export async function removeGroup(groupId) {
  const admin = await getAdmin();
  if (!adminRemoveGroup(admin, groupId)) return false;
  await save(admin);
  return true;
}

export async function hasGroup(groupId) {
  const admin = await getAdmin();
  return adminHasGroup(admin, groupId);
}

export async function getAllGroups() {
  const admin = await getAdmin();
  return admin.groups;
}
