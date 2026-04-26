import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /delgoogle <appId> — Java DeleteGoogleAppCommand.
export function createDeleteGoogleAppCommand(store) {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, store, sender))) return;
    const args = splitArgs(getCommandArguments(msg.text));
    if (args.length !== 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    try {
      const removed = await store.group.removeGoogleApp(msg.chat.id, args[0]);
      if (!removed) {
        await sender.sendMessage(msg.chat.id, 'Google app is not added');
        return;
      }
      await sender.sendMessage(msg.chat.id, 'Google app deleted successfully');
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}
