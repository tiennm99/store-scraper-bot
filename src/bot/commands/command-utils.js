import * as adminRepo from '../../repository/admin-repository.js';

export function splitArgs(text) {
  if (!text) return [];
  return text.trim().split(/\s+/).filter((s) => s.length > 0);
}

// Strips the "/<cmd>" or "/<cmd>@botname" prefix from message.text.
export function getCommandArguments(text) {
  if (!text) return '';
  const trimmed = text.trim();
  const space = trimmed.indexOf(' ');
  if (space < 0) return '';
  return trimmed.slice(space + 1).trim();
}

export async function authorizeGroup(chatId, sender) {
  try {
    const ok = await adminRepo.hasGroup(chatId);
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
