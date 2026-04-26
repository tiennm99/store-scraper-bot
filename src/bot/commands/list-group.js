import * as adminRepo from '../../repository/admin-repository.js';
import { getCommandArguments, requireAdminUser, splitArgs } from './command-utils.js';

// /listgroup — Java ListGroupCommand. Admin-only.
export function createListGroupCommand(config) {
  return async (msg, sender) => {
    if (!(await requireAdminUser(msg.from.id, msg.chat.id, config, sender))) return;
    if (splitArgs(getCommandArguments(msg.text)).length !== 0) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    try {
      const groups = await adminRepo.getAllGroups();
      if (groups.length === 0) {
        await sender.sendMessage(msg.chat.id, 'No groups found');
        return;
      }
      const lines = [`<b>Authorized groups (${groups.length}):</b>`];
      groups.forEach((gid, i) => lines.push(`${i + 1}. <code>${gid}</code>`));
      await sender.sendMessage(msg.chat.id, lines.join('\n') + '\n');
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}
