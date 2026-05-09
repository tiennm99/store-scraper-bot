import { requireAdminUser } from './command-utils.js';

const MIN_TTL = 60;
const MAX_TTL = 86400;

// /setappttl <n> — admin-only. Overrides upstream cache TTL (seconds).
// `0` or `default` clears the override (falls back to APP_CACHE_SECONDS env).
export function createSetAppTtlCommand(config, store) {
  return async (msg, sender, args) => {
    if (!(await requireAdminUser(msg.from.id, msg.chat.id, config, sender))) return;
    if (args.length !== 1) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    const arg = args[0];

    if (arg === 'default' || arg === '0') {
      await store.admin.setAppCacheSeconds(undefined);
      await sender.sendMessage(
        msg.chat.id,
        `Reset to default (${config.appCacheSeconds}s)`,
      );
      return;
    }

    const parsed = Number.parseInt(arg, 10);
    if (
      !Number.isFinite(parsed) ||
      String(parsed) !== arg ||
      parsed < MIN_TTL ||
      parsed > MAX_TTL
    ) {
      await sender.sendMessage(
        msg.chat.id,
        `Invalid arguments (allowed range: ${MIN_TTL}-${MAX_TTL}s)`,
      );
      return;
    }

    await store.admin.setAppCacheSeconds(parsed);
    await sender.sendMessage(msg.chat.id, `App cache TTL set to ${parsed}s`);
  };
}
