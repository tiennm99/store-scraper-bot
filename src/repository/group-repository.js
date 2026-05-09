import { del, getJson, putJson } from './upstash.js';
import {
  groupAddAppleApp,
  groupAddGoogleApp,
  groupIdToKey,
  groupRemoveAppleApp,
  groupRemoveGoogleApp,
  newGroup,
} from '../models/group.js';

// Upstash-backed per-group state. Logical key shape: `group:{chatId}`.
// Physical key gets KEY_PREFIX prepended by the adapter.
export function createGroupRepository(handle) {
  function key(groupId) {
    return `group:${groupIdToKey(groupId)}`;
  }

  async function exists(groupId) {
    const doc = await getJson(handle, key(groupId));
    return doc !== null;
  }

  async function getGroup(groupId) {
    const doc = await getJson(handle, key(groupId));
    return doc ?? newGroup(groupId);
  }

  async function saveGroup(group) {
    await putJson(handle, key(group._id), group);
  }

  async function initGroup(groupId) {
    if (await exists(groupId)) return;
    await saveGroup(newGroup(groupId));
  }

  async function deleteGroup(groupId) {
    await del(handle, key(groupId));
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
