import { buildGoogleRequest } from '../../api/google-scraper.js';
import * as groupRepo from '../../repository/group-repository.js';
import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /addgoogle <appId> [country=vn] — Java AddGoogleAppCommand.
export function createAddGoogleAppCommand(googleScraper) {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, sender))) return;
    const args = splitArgs(getCommandArguments(msg.text));
    if (args.length < 1 || args.length > 2) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const appId = args[0];
    const country = args.length === 2 ? args[1] : 'vn';

    let resp;
    try {
      resp = await googleScraper.fetchAndCache(buildGoogleRequest(appId, country));
    } catch {
      resp = null;
    }
    if (!resp) {
      await sender.sendMessage(msg.chat.id, 'Error when request app info');
      return;
    }

    try {
      const added = await groupRepo.addGoogleApp(msg.chat.id, appId, country);
      if (!added) {
        await sender.sendMessage(msg.chat.id, `Google app <code>${appId}</code> is already added`);
        return;
      }
      await sender.sendMessage(
        msg.chat.id,
        `Google app <code>${appId}</code>, country <b>${country}</b> added successfully`,
      );
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}
