import { buildAppleRequestByBundleId, buildAppleRequestByTrackId } from '../../api/apple-scraper.js';
import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /addapple <id|appId> [country=vn] — Java AddAppleAppCommand.
export function createAddAppleAppCommand(store, appleScraper) {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, store, sender))) return;
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

    let resp;
    try {
      resp = await appleScraper.fetchAndCache(req);
    } catch {
      resp = null;
    }
    if (!resp || !resp.appId) {
      await sender.sendMessage(msg.chat.id, 'Error when request app info');
      return;
    }

    try {
      const added = await store.group.addAppleApp(msg.chat.id, resp.appId, country);
      if (!added) {
        await sender.sendMessage(msg.chat.id, `Apple app <code>${resp.appId}</code> is already added`);
        return;
      }
      await sender.sendMessage(
        msg.chat.id,
        `Apple app <code>${resp.appId}</code>, country <b>${country}</b> added successfully`,
      );
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}
