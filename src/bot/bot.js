import { createTelegramApi } from './telegram-api.js';
import { COMMAND_CATALOG } from './commands/index.js';

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

  const ctx = { config, store, appleScraper, googleScraper };
  const commands = Object.fromEntries(
    COMMAND_CATALOG.map(({ name, build }) => [name, build(ctx)]),
  );

  return { sender, commands, api };
}
