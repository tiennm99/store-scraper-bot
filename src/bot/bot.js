import TelegramBot from 'node-telegram-bot-api';
import { createInfoCommand } from './commands/info.js';
import { createAddGroupCommand } from './commands/add-group.js';
import { createDeleteGroupCommand } from './commands/delete-group.js';
import { createListGroupCommand } from './commands/list-group.js';
import { createAddAppleAppCommand } from './commands/add-apple-app.js';
import { createDeleteAppleAppCommand } from './commands/delete-apple-app.js';
import { createAddGoogleAppCommand } from './commands/add-google-app.js';
import { createDeleteGoogleAppCommand } from './commands/delete-google-app.js';
import { createListAppCommand } from './commands/list-app.js';
import { createCheckAppCommand } from './commands/check-app.js';
import { createCheckAppScoresCommand } from './commands/check-app-scores.js';
import { createRawAppleAppCommand } from './commands/raw-apple-app.js';
import { createRawGoogleAppCommand } from './commands/raw-google-app.js';

// HTML parse mode for all messages (Java parity).
const PARSE_MODE = 'HTML';

export function createBot(config, appleScraper, googleScraper) {
  const tg = new TelegramBot(config.telegramBotToken, { polling: true });
  const logger = config.logger;

  const sender = {
    async sendMessage(chatId, html) {
      try {
        await tg.sendMessage(chatId, html, {
          parse_mode: PARSE_MODE,
          disable_web_page_preview: true,
        });
      } catch (err) {
        logger.warn({ chatId, err: err.message }, 'send message failed');
      }
    },
    async sendMessageSilent(chatId, html) {
      try {
        await tg.sendMessage(chatId, html, {
          parse_mode: PARSE_MODE,
          disable_web_page_preview: true,
          disable_notification: true,
        });
      } catch (err) {
        logger.warn({ chatId, err: err.message }, 'send silent message failed');
      }
    },
    async sendDocument(chatId, filename, body) {
      try {
        await tg.sendDocument(
          chatId,
          Buffer.from(body, 'utf8'),
          {},
          { filename, contentType: 'application/json' },
        );
      } catch (err) {
        logger.warn({ chatId, err: err.message }, 'send document failed');
      }
    },
  };

  // Java command identifiers — keep names matching exactly.
  const commands = {
    info: createInfoCommand(),
    addgroup: createAddGroupCommand(config),
    delgroup: createDeleteGroupCommand(config),
    listgroup: createListGroupCommand(config),
    addapple: createAddAppleAppCommand(appleScraper),
    delapple: createDeleteAppleAppCommand(),
    addgoogle: createAddGoogleAppCommand(googleScraper),
    delgoogle: createDeleteGoogleAppCommand(),
    listapp: createListAppCommand(),
    checkapp: createCheckAppCommand(config, appleScraper, googleScraper),
    checkappscore: createCheckAppScoresCommand(appleScraper, googleScraper),
    rawappleapp: createRawAppleAppCommand(appleScraper),
    rawgoogleapp: createRawGoogleAppCommand(googleScraper),
  };

  tg.on('message', async (msg) => {
    const name = parseCommandName(msg.text, config.telegramBotUsername);
    if (!name) return;
    const handler = commands[name];
    if (!handler) {
      logger.debug({ command: name }, 'Unknown command');
      return;
    }
    logger.info(
      { command: name, userId: msg.from?.id, chatId: msg.chat.id },
      'Executing command',
    );
    try {
      await handler(msg, sender);
    } catch (err) {
      logger.error({ err: err.message, command: name }, 'panic in command');
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  });

  tg.on('polling_error', (err) => {
    logger.warn({ err: err.message }, 'polling error');
  });

  tg.getMe()
    .then((me) => logger.info({ username: me.username }, 'Authorized on account'))
    .catch((err) => logger.error({ err: err.message }, 'getMe failed'));

  return { sender, telegram: tg };
}

// Extracts "info" from "/info", "/info arg", "/info@bot", "/info@bot arg".
function parseCommandName(text, botUsername) {
  if (!text || text[0] !== '/') return null;
  const space = text.indexOf(' ');
  const head = space < 0 ? text.slice(1) : text.slice(1, space);
  const at = head.indexOf('@');
  if (at < 0) return head;
  const cmd = head.slice(0, at);
  const target = head.slice(at + 1);
  if (botUsername && target && target.toLowerCase() !== botUsername.toLowerCase()) return null;
  return cmd;
}
