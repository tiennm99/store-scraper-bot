import { createTelegramApi } from './telegram-api.js';
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

export function createBot(config, store, appleScraper, googleScraper) {
  const api = createTelegramApi(config.telegramBotToken);
  const logger = config.logger;

  const sender = {
    async sendMessage(chatId, html) {
      try {
        await api.sendMessage(chatId, html, {
          parse_mode: PARSE_MODE,
          disable_web_page_preview: true,
        });
      } catch (err) {
        logger.warn({ chatId, err: err.message }, 'send message failed');
      }
    },
    async sendMessageSilent(chatId, html) {
      try {
        await api.sendMessage(chatId, html, {
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
        await api.sendDocument(chatId, filename, body);
      } catch (err) {
        logger.warn({ chatId, err: err.message }, 'send document failed');
      }
    },
  };

  // Java command identifiers — keep names matching exactly.
  const commands = {
    info: createInfoCommand(),
    addgroup: createAddGroupCommand(config, store),
    delgroup: createDeleteGroupCommand(config, store),
    listgroup: createListGroupCommand(config, store),
    addapple: createAddAppleAppCommand(store, appleScraper),
    delapple: createDeleteAppleAppCommand(store),
    addgoogle: createAddGoogleAppCommand(store, googleScraper),
    delgoogle: createDeleteGoogleAppCommand(store),
    listapp: createListAppCommand(store),
    checkapp: createCheckAppCommand(config, store, appleScraper, googleScraper),
    checkappscore: createCheckAppScoresCommand(store, appleScraper, googleScraper),
    rawappleapp: createRawAppleAppCommand(appleScraper),
    rawgoogleapp: createRawGoogleAppCommand(googleScraper),
  };

  return { sender, commands, api };
}
