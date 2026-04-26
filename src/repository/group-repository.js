import { getCollection } from './mongodb.js';
import {
  groupAddAppleApp,
  groupAddGoogleApp,
  groupIdToKey,
  groupRemoveAppleApp,
  groupRemoveGoogleApp,
  newGroup,
} from '../models/group.js';

export function createGroupRepository(env) {
  function collection() {
    return getCollection('group', env);
  }

  async function exists(groupId) {
    const c = await collection();
    const count = await c.countDocuments({ _id: groupIdToKey(groupId) });
    return count > 0;
  }

  async function getGroup(groupId) {
    const c = await collection();
    const doc = await c.findOne({ _id: groupIdToKey(groupId) });
    return doc ?? newGroup(groupId);
  }

  async function saveGroup(group) {
    const c = await collection();
    await c.replaceOne({ _id: group._id }, group, { upsert: true });
  }

  async function initGroup(groupId) {
    if (await exists(groupId)) return;
    await saveGroup(newGroup(groupId));
  }

  async function deleteGroup(groupId) {
    const c = await collection();
    await c.deleteOne({ _id: groupIdToKey(groupId) });
  }

  async function mutateAndSave(groupId, mutator) {
    const group = await getGroup(groupId);
    if (!mutator(group)) return false;
    await saveGroup(group);
    return true;
  }

  return {
    exists,
    getGroup,
    saveGroup,
    initGroup,
    deleteGroup,
    addAppleApp: (groupId, appId, country) =>
      mutateAndSave(groupId, (g) => groupAddAppleApp(g, appId, country)),
    removeAppleApp: (groupId, appId) =>
      mutateAndSave(groupId, (g) => groupRemoveAppleApp(g, appId)),
    addGoogleApp: (groupId, appId, country) =>
      mutateAndSave(groupId, (g) => groupAddGoogleApp(g, appId, country)),
    removeGoogleApp: (groupId, appId) =>
      mutateAndSave(groupId, (g) => groupRemoveGoogleApp(g, appId)),
  };
}
