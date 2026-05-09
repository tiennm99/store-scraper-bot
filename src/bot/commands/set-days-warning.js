import { authorizeGroup } from './command-utils.js';

const MAX_DAYS = 3650;

// /setdayswarning <n> — sets the per-group warning threshold.
// `0` or `default` resets to the env-config default.
export function createSetDaysWarningCommand(config, store) {
  return async (msg, sender, args) => {
    if (!(await authorizeGroup(msg.chat.id, store, sender))) return;
    if (args.length !== 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const arg = args[0];

    if (arg === 'default' || arg === '0') {
      await store.group.setSetting(msg.chat.id, 'numDaysWarningNotUpdated', undefined);
      await sender.sendMessage(
        msg.chat.id,
        `Reset to default (${config.numDaysWarningNotUpdated}d)`,
      );
      return;
    }

    const parsed = Number.parseInt(arg, 10);
    if (!Number.isFinite(parsed) || String(parsed) !== arg || parsed < 1 || parsed > MAX_DAYS) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }

    await store.group.setSetting(msg.chat.id, 'numDaysWarningNotUpdated', parsed);
    await sender.sendMessage(msg.chat.id, `Days-to-warning set to ${parsed}`);
  };
}
