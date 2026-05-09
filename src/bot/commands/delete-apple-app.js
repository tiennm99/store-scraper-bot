import { authorizeGroup } from './command-utils.js';

// /delapple <appId>
export function createDeleteAppleAppCommand(store) {
  return async (msg, sender, args) => {
    if (!(await authorizeGroup(msg.chat.id, store, sender))) return;
    if (args.length !== 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const removed = await store.group.removeAppleApp(msg.chat.id, args[0]);
    if (!removed) {
      await sender.sendMessage(msg.chat.id, 'Apple app is not added');
      return;
    }
    await sender.sendMessage(msg.chat.id, 'Apple app deleted successfully');
  };
}
