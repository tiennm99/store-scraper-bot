import { getCollection } from './mongodb.js';
import {
  groupAddAppleApp,
  groupAddGoogleApp,
  groupIdToKey,
  groupRemoveAppleApp,
  groupRemoveGoogleApp,
  newGroup,
} from '../models/group.js';

function collection() {
  return getCollection('group');
}

export async function exists(groupId) {
  const count = await collection().countDocuments({ _id: groupIdToKey(groupId) });
  return count > 0;
}

export async function getGroup(groupId) {
  const doc = await collection().findOne({ _id: groupIdToKey(groupId) });
  return doc ?? newGroup(groupId);
}

export async function saveGroup(group) {
  await collection().replaceOne({ _id: group._id }, group, { upsert: true });
}

export async function initGroup(groupId) {
  if (await exists(groupId)) return;
  await saveGroup(newGroup(groupId));
}

export async function deleteGroup(groupId) {
  await collection().deleteOne({ _id: groupIdToKey(groupId) });
}

async function mutateAndSave(groupId, mutator) {
  const group = await getGroup(groupId);
  if (!mutator(group)) return false;
  await saveGroup(group);
  return true;
}

export function addAppleApp(groupId, appId, country) {
  return mutateAndSave(groupId, (g) => groupAddAppleApp(g, appId, country));
}

export function removeAppleApp(groupId, appId) {
  return mutateAndSave(groupId, (g) => groupRemoveAppleApp(g, appId));
}

export function addGoogleApp(groupId, appId, country) {
  return mutateAndSave(groupId, (g) => groupAddGoogleApp(g, appId, country));
}

export function removeGoogleApp(groupId, appId) {
  return mutateAndSave(groupId, (g) => groupRemoveGoogleApp(g, appId));
}
