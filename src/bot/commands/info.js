import { getCommandArguments, splitArgs } from './command-utils.js';

// /info — Java InfoCommand. Reports the chat (group) ID.
export function createInfoCommand() {
  return async (msg, sender) => {
    if (splitArgs(getCommandArguments(msg.text)).length !== 0) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    await sender.sendMessage(msg.chat.id, `Id của nhóm là <code>${msg.chat.id}</code>\n`);
  };
}
