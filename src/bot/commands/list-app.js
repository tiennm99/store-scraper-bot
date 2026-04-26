import * as groupRepo from '../../repository/group-repository.js';
import { buildTable } from '../../util/table.js';
import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /listapp — Java ListAppCommand. Two tables (Apple / Google) of tracked apps.
export function createListAppCommand() {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, sender))) return;
    if (splitArgs(getCommandArguments(msg.text)).length !== 0) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    try {
      const group = await groupRepo.getGroup(msg.chat.id);
      const out =
        '<b>Apple Apps</b>\n' +
        formatAppTable(group.appleApps) +
        '\n<b>Google Apps</b>\n' +
        formatAppTable(group.googleApps);
      await sender.sendMessage(msg.chat.id, out);
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}

function formatAppTable(apps) {
  if (apps.length === 0) return '<i>(none)</i>\n';
  const rows = apps.map((a, i) => [String(i + 1), a.appId, a.country]);
  return `<pre>${buildTable(['#', 'AppId', 'Country'], rows)}</pre>\n`;
}
