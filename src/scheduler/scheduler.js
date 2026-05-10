import { buildTable } from '../util/table.js';
import { daysBetween, formatDateInTz, weekdayInTz } from '../util/time.js';
import { resolveDaysWarning } from '../util/group-settings.js';

// One-shot daily check, invoked from api/cron.js. The cron schedule lives in
// vercel.json ("0 0 * * *" UTC = 7am Asia/Ho_Chi_Minh).
export async function runDailyCheck(config, store, sender, appleScraper, googleScraper) {
  const logger = config.logger;
  const now = new Date();
  const dow = weekdayInTz(now, config.timezone);
  const silent = dow === 0 || dow === 6;
  logger.info({ silent }, 'Running daily check job');

  let groups;
  try {
    groups = await store.admin.getAllGroups();
  } catch (err) {
    logger.error({ err: err.message }, 'Failed to get groups');
    return;
  }

  for (const gid of groups) {
    try {
      await checkGroup(gid, silent, now, config, store, sender, appleScraper, googleScraper);
    } catch (err) {
      logger.error({ err: err.message, groupId: gid }, 'check group failed');
    }
  }
  logger.info({ groupsChecked: groups.length }, 'Daily check job completed');
}

async function checkGroup(groupId, silent, now, config, store, sender, appleScraper, googleScraper) {
  const logger = config.logger;
  const group = await store.group.getGroup(groupId);
  if (group.appleApps.length === 0 && group.googleApps.length === 0) {
    logger.info({ groupId }, 'Group has no apps, skipping');
    return;
  }

  const threshold = resolveDaysWarning(group, config);
  const staleApple = [];
  const staleGoogle = [];

  for (const info of group.appleApps) {
    try {
      const app = await appleScraper.getApp(info.appId, info.country);
      if (!app) continue;
      const updatedMs = Date.parse(app.updated);
      if (Number.isNaN(updatedMs)) continue;
      const days = daysBetween(updatedMs, now.getTime());
      if (days > threshold) {
        staleApple.push({
          appId: info.appId,
          days,
          updated: formatDateInTz(new Date(updatedMs), config.timezone),
        });
      }
    } catch (err) {
      logger.error({ err: err.message, appId: info.appId }, 'Apple fetch failed');
    }
  }

  for (const info of group.googleApps) {
    try {
      const app = await googleScraper.getApp(info.appId, info.country);
      if (!app) continue;
      const updatedMs = Number(app.updated);
      if (!Number.isFinite(updatedMs)) continue;
      const days = daysBetween(updatedMs, now.getTime());
      if (days > threshold) {
        staleGoogle.push({
          appId: info.appId,
          days,
          updated: formatDateInTz(new Date(updatedMs), config.timezone),
        });
      }
    } catch (err) {
      logger.error({ err: err.message, appId: info.appId }, 'Google fetch failed');
    }
  }

  const total = staleApple.length + staleGoogle.length;
  if (total === 0) {
    logger.info({ groupId }, 'All apps up-to-date');
    return;
  }
  const message = buildReport(staleApple, staleGoogle);
  if (silent) await sender.sendMessageSilent(groupId, message);
  else await sender.sendMessage(groupId, message);
}

function buildReport(staleApple, staleGoogle) {
  const total = staleApple.length + staleGoogle.length;
  const headers = ['#', 'AppId', 'Updated', 'Days'];
  let out = `You have ${total} app(s) need to be updated!\n`;
  if (staleApple.length > 0) {
    const rows = staleApple.map((a, i) => [String(i + 1), a.appId, a.updated, String(a.days)]);
    out += `<b>${staleApple.length} Apple Apps:</b>\n<code>\n${buildTable(headers, rows)}\n</code>\n`;
  }
  if (staleGoogle.length > 0) {
    const rows = staleGoogle.map((a, i) => [String(i + 1), a.appId, a.updated, String(a.days)]);
    out += `<b>${staleGoogle.length} Google Apps:</b>\n<code>\n${buildTable(headers, rows)}\n</code>`;
  }
  return out;
}
