import { authorizeGroup } from './command-utils.js';

// /delgoogle <appId>
export function createDeleteGoogleAppCommand(store) {
  return async (msg, sender, args) => {
    if (!(await authorizeGroup(msg.chat.id, store, sender))) return;
    if (args.length !== 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const removed = await store.group.removeGoogleApp(msg.chat.id, args[0]);
    if (!removed) {
      await sender.sendMessage(msg.chat.id, 'Google app is not added');
      return;
    }
    await sender.sendMessage(msg.chat.id, 'Google app deleted successfully');
  };
}
