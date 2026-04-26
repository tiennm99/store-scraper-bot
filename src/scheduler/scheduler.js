import { buildTable, formatNumber, truncateString } from '../util/table.js';
import { daysBetween, formatDateInTz, formatDateTimeInTz, weekdayInTz } from '../util/time.js';

// One-shot daily check, invoked from the Worker `scheduled` handler. The cron
// schedule lives in wrangler.toml ("0 0 * * *" UTC = 7am Asia/Ho_Chi_Minh).
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

  const threshold = config.numDaysWarningNotUpdated;
  const stale = [];

  for (const info of group.appleApps) {
    try {
      const app = await appleScraper.getApp(info.appId, info.country);
      if (!app) continue;
      const updatedMs = Date.parse(app.updated);
      if (Number.isNaN(updatedMs)) continue;
      const days = daysBetween(updatedMs, now.getTime());
      if (days > threshold) {
        stale.push({
          appId: info.appId,
          title: app.title,
          days,
          updated: formatDateInTz(new Date(updatedMs), config.timezone),
          score: app.score,
          reviews: Number(app.reviews ?? 0),
          ratings: Number(app.ratings ?? 0),
          isApple: true,
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
      const days = daysBetween(updatedMs, now.getTime());
      if (days > threshold) {
        stale.push({
          appId: info.appId,
          title: app.title,
          days,
          updated: formatDateInTz(new Date(updatedMs), config.timezone),
          score: app.score,
          reviews: Number(app.reviews ?? 0),
          ratings: Number(app.ratings ?? 0),
          isApple: false,
        });
      }
    } catch (err) {
      logger.error({ err: err.message, appId: info.appId }, 'Google fetch failed');
    }
  }

  if (stale.length === 0) {
    logger.info({ groupId }, 'All apps up-to-date');
    return;
  }
  const message = buildReport(groupId, stale, now, config);
  if (silent) await sender.sendMessageSilent(groupId, message);
  else await sender.sendMessage(groupId, message);
}

function buildReport(groupId, apps, now, config) {
  const headers = ['App', 'Store', 'Days', 'Updated', 'Score', 'Reviews', 'Ratings'];
  const rows = apps.map((a) => [
    truncateString(a.title || '', 30),
    a.isApple ? 'Apple' : 'Google',
    String(a.days),
    a.updated,
    Number(a.score ?? 0).toFixed(1),
    String(a.reviews),
    formatNumber(a.ratings),
  ]);
  return (
    `<b>Daily App Check Report</b>\n` +
    `Date: ${formatDateTimeInTz(now, config.timezone)}\n` +
    `Group: <code>${groupId}</code>\n` +
    `Apps not updated in &gt;${config.numDaysWarningNotUpdated} days: <b>${apps.length}</b>\n\n` +
    `<pre>${buildTable(headers, rows)}</pre>`
  );
}
