// Per-message dispatcher. Routes Telegram update messages to the matching
// command handler. Equivalent to the inner loop of the old polling bot.
export async function dispatch(message, deps) {
  const { sender, commands, config, logger } = deps;
  if (!message?.text || message.text[0] !== '/') return;

  const name = parseCommandName(message.text, config.telegramBotUsername);
  if (!name) return;

  const handler = commands[name];
  if (!handler) {
    logger.debug({ command: name }, 'Unknown command');
    return;
  }

  logger.info(
    { command: name, userId: message.from?.id, chatId: message.chat.id },
    'Executing command',
  );
  try {
    await handler(message, sender);
  } catch (err) {
    logger.error({ err: err.message, command: name }, 'command failed');
    try {
      await sender.sendMessage(message.chat.id, 'Internal server error');
    } catch {
      // best-effort; don't double-log
    }
  }
}

// Extracts "info" from "/info", "/info arg", "/info@bot", "/info@bot arg".
function parseCommandName(text, botUsername) {
  const space = text.indexOf(' ');
  const head = space < 0 ? text.slice(1) : text.slice(1, space);
  const at = head.indexOf('@');
  if (at < 0) return head;
  const cmd = head.slice(0, at);
  const target = head.slice(at + 1);
  if (botUsername && target && target.toLowerCase() !== botUsername.toLowerCase()) return null;
  return cmd;
}
