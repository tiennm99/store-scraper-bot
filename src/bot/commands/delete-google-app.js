import * as groupRepo from '../../repository/group-repository.js';
import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /delgoogle <appId> — Java DeleteGoogleAppCommand.
export function createDeleteGoogleAppCommand() {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, sender))) return;
    const args = splitArgs(getCommandArguments(msg.text));
    if (args.length !== 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    try {
      const removed = await groupRepo.removeGoogleApp(msg.chat.id, args[0]);
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
