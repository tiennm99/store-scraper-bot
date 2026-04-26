import { getCommandArguments, requireAdminUser, splitArgs } from './command-utils.js';

// /addgroup [groupId] — Java AddGroupCommand. Admin-only.
export function createAddGroupCommand(config, store) {
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
      const added = await store.admin.addGroup(groupId);
      if (!added) {
        await sender.sendMessage(msg.chat.id, 'Group is already added');
        return;
      }
      await store.group.initGroup(groupId);
      await sender.sendMessage(msg.chat.id, 'Group added successfully');
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}
