import { loadConfig } from './config.js';
import { closeMongoDB, initMongoDB } from './repository/mongodb.js';
import { initAdmin } from './repository/admin-repository.js';
import { createAppleScraper } from './api/apple-scraper.js';
import { createGoogleScraper } from './api/google-scraper.js';
import { createBot } from './bot/bot.js';
import { createScheduler } from './scheduler/scheduler.js';

async function main() {
  const config = loadConfig();
  const logger = config.logger;
  logger.info({ env: config.env, commit: config.sourceCommit }, 'Starting Store Scraper Bot');

  await initMongoDB(config);
  await initAdmin(); // Java parity: ensure singleton "common/admin" doc exists.

  const appleScraper = createAppleScraper(config);
  const googleScraper = createGoogleScraper(config);

  const bot = createBot(config, appleScraper, googleScraper);
  const scheduler = createScheduler(config, bot.sender, appleScraper, googleScraper);
  scheduler.start();

  logger.info('Starting Telegram bot polling');

  const shutdown = async (signal) => {
    logger.info({ signal }, 'Received shutdown signal, stopping bot...');
    try {
      scheduler.stop();
      await bot.telegram.stopPolling();
      await closeMongoDB();
    } finally {
      process.exit(0);
    }
  };
  process.on('SIGINT', () => shutdown('SIGINT'));
  process.on('SIGTERM', () => shutdown('SIGTERM'));
}

main().catch((err) => {
  console.error('Fatal:', err);
  process.exit(1);
});
