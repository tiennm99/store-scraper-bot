import * as groupRepo from '../../repository/group-repository.js';
import { buildTable } from '../../util/table.js';
import { daysBetween, formatDateInTz } from '../../util/time.js';
import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /checkapp — Java CheckAppCommand. Reports update status per app, per store.
export function createCheckAppCommand(config, appleScraper, googleScraper) {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, sender))) return;
    if (splitArgs(getCommandArguments(msg.text)).length !== 0) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    try {
      const group = await groupRepo.getGroup(msg.chat.id);
      const nowMs = Date.now();
      const threshold = config.numDaysWarningNotUpdated;
      const headers = ['AppId', 'Updated', 'Days', 'OK'];

      const appleRows = await appleRowsFor(group.appleApps, appleScraper, nowMs, threshold, config.timezone);
      const googleRows = await googleRowsFor(group.googleApps, googleScraper, nowMs, threshold, config.timezone);

      const out =
        '<b>Apple Apps</b>\n' +
        renderTable(appleRows, headers) +
        '\n<b>Google Apps</b>\n' +
        renderTable(googleRows, headers);
      await sender.sendMessage(msg.chat.id, out);
    } catch {
      await sender.sendMessage(msg.chat.id, 'Internal server error');
    }
  };
}

async function appleRowsFor(apps, scraper, nowMs, threshold, timezone) {
  const rows = [];
  for (const a of apps) {
    try {
      const resp = await scraper.getApp(a.appId, a.country);
      if (!resp) {
        rows.push([a.appId, '?', '?', mark(false)]);
        continue;
      }
      const updatedMs = Date.parse(resp.updated);
      if (Number.isNaN(updatedMs)) {
        rows.push([a.appId, resp.updated || '?', '?', mark(false)]);
        continue;
      }
      const days = daysBetween(updatedMs, nowMs);
      rows.push([a.appId, formatDateInTz(new Date(updatedMs), timezone), String(days), mark(days <= threshold)]);
    } catch {
      rows.push([a.appId, '?', '?', mark(false)]);
    }
  }
  return rows;
}

async function googleRowsFor(apps, scraper, nowMs, threshold, timezone) {
  const rows = [];
  for (const a of apps) {
    try {
      const resp = await scraper.getApp(a.appId, a.country);
      if (!resp) {
        rows.push([a.appId, '?', '?', mark(false)]);
        continue;
      }
      const updatedMs = Number(resp.updated);
      const days = daysBetween(updatedMs, nowMs);
      rows.push([a.appId, formatDateInTz(new Date(updatedMs), timezone), String(days), mark(days <= threshold)]);
    } catch {
      rows.push([a.appId, '?', '?', mark(false)]);
    }
  }
  return rows;
}

function renderTable(rows, headers) {
  if (rows.length === 0) return '<i>(none)</i>\n';
  return `<pre>${buildTable(headers, rows)}</pre>\n`;
}

function mark(ok) {
  return ok ? '✅' : '❌';
}
