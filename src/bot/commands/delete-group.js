import { requireAdminUser } from './command-utils.js';

// /delgroup [groupId] — admin-only. Removes from allowlist AND wipes the
// group's tracked-app state to avoid orphaned `group:{chatId}` keys.
export function createDeleteGroupCommand(config, store) {
  return async (msg, sender, args) => {
    if (!(await requireAdminUser(msg.from.id, msg.chat.id, config, sender))) return;
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
    const removed = await store.admin.removeGroup(groupId);
    if (!removed) {
      await sender.sendMessage(msg.chat.id, 'Group is not added');
      return;
    }
    await store.group.deleteGroup(groupId);
    await sender.sendMessage(msg.chat.id, 'Group deleted successfully');
  };
}
