import { del, getJson, putJson } from './upstash.js';

// Upstash-backed per-group state. Logical key shape: `group:{chatId}`.
export function createGroupRepository(handle) {
  function key(groupId) {
    return `group:${String(groupId)}`;
  }

  function newGroup(groupId) {
    return { _id: String(groupId), appleApps: [], googleApps: [] };
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

  function addApp(list, appId, country) {
    if (list.some((a) => a.appId === appId)) return false;
    list.push({ appId, country });
    return true;
  }

  function removeApp(list, appId) {
    const i = list.findIndex((a) => a.appId === appId);
    if (i < 0) return false;
    list.splice(i, 1);
    return true;
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
      mutateAndSave(groupId, (g) => addApp(g.appleApps, appId, country)),
    removeAppleApp: (groupId, appId) =>
      mutateAndSave(groupId, (g) => removeApp(g.appleApps, appId)),
    addGoogleApp: (groupId, appId, country) =>
      mutateAndSave(groupId, (g) => addApp(g.googleApps, appId, country)),
    removeGoogleApp: (groupId, appId) =>
      mutateAndSave(groupId, (g) => removeApp(g.googleApps, appId)),
  };
}
