import { del, getJson, putJson } from './upstash.js';

// Upstash-backed per-group state. Logical key shape: `group:{chatId}`.
export function createGroupRepository(handle) {
  function key(groupId) {
    return `group:${groupId}`;
  }

  function emptyGroup() {
    return { appleApps: [], googleApps: [] };
  }

  async function getGroup(groupId) {
    return (await getJson(handle, key(groupId))) ?? emptyGroup();
  }

  async function save(groupId, group) {
    await putJson(handle, key(groupId), group);
  }

  async function initGroup(groupId) {
    if (await getJson(handle, key(groupId))) return;
    await save(groupId, emptyGroup());
  }

  async function deleteGroup(groupId) {
    await del(handle, key(groupId));
  }

  async function mutateAndSave(groupId, mutator) {
    const group = await getGroup(groupId);
    if (!mutator(group)) return false;
    await save(groupId, group);
    return true;
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

  return {
    getGroup,
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
