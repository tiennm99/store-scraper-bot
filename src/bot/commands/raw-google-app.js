import { buildGoogleRequest } from '../../api/google-scraper.js';
import { getCommandArguments, splitArgs } from './command-utils.js';

// /rawgoogleapp <appId> [country=vn] — Java RawGoogleAppCommand.
export function createRawGoogleAppCommand(googleScraper) {
  return async (msg, sender) => {
    const args = splitArgs(getCommandArguments(msg.text));
    if (args.length < 1 || args.length > 2) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const appId = args[0];
    const country = args.length === 2 ? args[1] : 'vn';

    let raw;
    try {
      raw = await googleScraper.rawApp(buildGoogleRequest(appId, country));
    } catch {
      raw = '';
    }
    if (!raw) {
      await sender.sendMessage(msg.chat.id, 'Error when request app info');
      return;
    }
    await sender.sendDocument(msg.chat.id, `${appId}.json`, raw);
  };
}
