// Per-message dispatcher. Routes Telegram update messages to the matching
// command handler. Equivalent to the inner loop of the old polling bot.
export async function dispatch(message, deps) {
  const { sender, commands, config, logger } = deps;
  if (!message?.text || message.text[0] !== '/') return;

  const parsed = parseCommand(message.text, config.telegramBotUsername);
  if (!parsed) return;
  const { name, args } = parsed;

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
    await handler(message, sender, args);
  } catch (err) {
    logger.error({ err: err.message, command: name }, 'command failed');
    try {
      await sender.sendMessage(message.chat.id, 'Internal server error');
    } catch {
      // best-effort; don't double-log
    }
  }
}

// Extracts name + args from "/cmd", "/cmd arg", "/cmd@bot", "/cmd@bot arg".
// Returns null if the @bot suffix targets a different bot.
function parseCommand(text, botUsername) {
  const trimmed = text.trim();
  const space = trimmed.indexOf(' ');
  const head = space < 0 ? trimmed.slice(1) : trimmed.slice(1, space);
  const argText = space < 0 ? '' : trimmed.slice(space + 1).trim();

  const at = head.indexOf('@');
  let name = head;
  if (at >= 0) {
    name = head.slice(0, at);
    const target = head.slice(at + 1);
    if (botUsername && target && target.toLowerCase() !== botUsername.toLowerCase()) return null;
  }

  const args = argText ? argText.split(/\s+/) : [];
  return { name, args };
}
