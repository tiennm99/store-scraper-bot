export async function authorizeGroup(chatId, store, sender) {
  try {
    const ok = await store.admin.hasGroup(chatId);
    if (!ok) {
      await sender.sendMessage(chatId, 'Group is not allowed to use bot');
      return false;
    }
    return true;
  } catch {
    await sender.sendMessage(chatId, 'Group is not allowed to use bot');
    return false;
  }
}

export async function requireAdminUser(userId, chatId, config, sender) {
  if (!config.isAdmin(userId)) {
    await sender.sendMessage(chatId, 'You are not authorized to use this command');
    return false;
  }
  return true;
}
