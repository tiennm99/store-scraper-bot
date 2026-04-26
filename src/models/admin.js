// Admin singleton document — Java parity (_id="admin", class="Admin").
export const ADMIN_ID = 'admin';

export function newAdmin() {
  return { _id: ADMIN_ID, class: 'Admin', groups: [] };
}

export function adminAddGroup(admin, groupId) {
  if (admin.groups.includes(groupId)) return false;
  admin.groups.push(groupId);
  return true;
}

export function adminRemoveGroup(admin, groupId) {
  const i = admin.groups.indexOf(groupId);
  if (i < 0) return false;
  admin.groups.splice(i, 1);
  return true;
}

export function adminHasGroup(admin, groupId) {
  return admin.groups.includes(groupId);
}
