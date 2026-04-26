import * as groupRepo from '../../repository/group-repository.js';
import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /delapple <appId> — Java DeleteAppleAppCommand.
export function createDeleteAppleAppCommand() {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, sender))) return;
    const args = splitArgs(getCommandArguments(msg.text));
    if (args.length !== 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    try {
      const removed = await groupRepo.removeAppleApp(msg.chat.id, args[0]);
      if (!removed) {
        await sender.sendMessage(msg.chat.id, 'Apple app is not added');
        return;
      }
      await sender.sendMessage(msg.chat.id, 'Apple app deleted successfully');
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}
