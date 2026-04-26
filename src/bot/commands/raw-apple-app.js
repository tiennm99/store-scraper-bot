import { buildAppleRequestByBundleId, buildAppleRequestByTrackId } from '../../api/apple-scraper.js';
import { getCommandArguments, splitArgs } from './command-utils.js';

// /rawappleapp <id|appId> [country=vn] — Java RawAppleAppCommand.
// Sends raw upstream JSON as a Telegram document attachment.
export function createRawAppleAppCommand(appleScraper) {
  return async (msg, sender) => {
    const args = splitArgs(getCommandArguments(msg.text));
    if (args.length < 1 || args.length > 2) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const country = args.length === 2 ? args[1] : 'vn';
    const trackId = Number.parseInt(args[0], 10);
    const req =
      Number.isFinite(trackId) && String(trackId) === args[0]
        ? buildAppleRequestByTrackId(trackId, country)
        : buildAppleRequestByBundleId(args[0], country);

    let raw;
    try {
      raw = await appleScraper.rawApp(req);
    } catch {
      raw = '';
    }
    if (!raw) {
      await sender.sendMessage(msg.chat.id, 'Error when request app info');
      return;
    }
    await sender.sendDocument(msg.chat.id, `${args[0]}.json`, raw);
  };
}
