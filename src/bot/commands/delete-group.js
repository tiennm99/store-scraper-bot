import * as adminRepo from '../../repository/admin-repository.js';
import { getCommandArguments, requireAdminUser, splitArgs } from './command-utils.js';

// /delgroup [groupId] — Java DeleteGroupCommand. Admin-only.
export function createDeleteGroupCommand(config) {
  return async (msg, sender) => {
    if (!(await requireAdminUser(msg.from.id, msg.chat.id, config, sender))) return;
    const args = splitArgs(getCommandArguments(msg.text));
    if (args.length > 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    let groupId = msg.chat.id;
    if (args.length === 1) {
      const parsed = Number.parseInt(args[0], 10);
      if (!Number.isFinite(parsed)) {
        await sender.sendMessage(msg.chat.id, 'Invalid arguments');
        return;
      }
      groupId = parsed;
    }
    try {
      const removed = await adminRepo.removeGroup(groupId);
      if (!removed) {
        await sender.sendMessage(msg.chat.id, 'Group is not added');
        return;
      }
      await sender.sendMessage(msg.chat.id, 'Group deleted successfully');
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}
