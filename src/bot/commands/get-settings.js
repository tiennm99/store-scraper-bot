import { buildTable } from '../../util/table.js';
import { authorizeGroup } from './command-utils.js';

// /settings — table of per-group setting overrides + their env defaults,
// plus admin-global runtime overrides (cache TTL).
export function createGetSettingsCommand(config, store) {
  return async (msg, sender, args) => {
    if (!(await authorizeGroup(msg.chat.id, store, sender))) return;
    if (args.length !== 0) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const group = await store.group.getGroup(msg.chat.id);
    const s = group?.settings ?? {};
    const adminTtl = await store.admin.getAppCacheSeconds();
    const rows = [
      [
        'numDaysWarningNotUpdated',
        formatValue(s.numDaysWarningNotUpdated),
        String(config.numDaysWarningNotUpdated),
      ],
      [
        'appCacheSeconds (admin)',
        adminTtl === undefined ? '(unset)' : `${adminTtl}`,
        String(config.appCacheSeconds),
      ],
    ];
    const out =
      '<b>Group Settings</b>\n' +
      `<pre>${buildTable(['Setting', 'Value', 'Default'], rows)}</pre>`;
    await sender.sendMessage(msg.chat.id, out);
  };
}

function formatValue(v) {
  return v === undefined || v === null ? '(unset)' : String(v);
}
