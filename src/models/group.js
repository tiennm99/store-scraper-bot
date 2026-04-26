// Group document — Java parity (_id is string form of Telegram chat ID).
export function groupIdToKey(groupId) {
  return String(groupId);
}

export function groupKeyToId(key) {
  return Number(key);
}

export function newGroup(groupId) {
  return {
    _id: groupIdToKey(groupId),
    class: 'Group',
    appleApps: [],
    googleApps: [],
  };
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

export function groupAddAppleApp(group, appId, country) {
  return addApp(group.appleApps, appId, country);
}

export function groupRemoveAppleApp(group, appId) {
  return removeApp(group.appleApps, appId);
}

export function groupAddGoogleApp(group, appId, country) {
  return addApp(group.googleApps, appId, country);
}

export function groupRemoveGoogleApp(group, appId) {
  return removeApp(group.googleApps, appId);
}
