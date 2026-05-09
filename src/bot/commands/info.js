// /info — replies with the current chat ID (used to obtain the group ID for /addgroup).
export function createInfoCommand() {
  return async (msg, sender, args) => {
    if (args.length !== 0) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    await sender.sendMessage(msg.chat.id, `Id của nhóm là <code>${msg.chat.id}</code>\n`);
  };
}
