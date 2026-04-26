import * as groupRepo from '../../repository/group-repository.js';
import { buildTable } from '../../util/table.js';
import { authorizeGroup, getCommandArguments, splitArgs } from './command-utils.js';

// /checkappscore — Java CheckAppScoreCommand. Reports score + ratings.
// Score rounded to 1 decimal (Java Precision.round(score, 1) parity).
export function createCheckAppScoresCommand(appleScraper, googleScraper) {
  return async (msg, sender) => {
    if (!(await authorizeGroup(msg.chat.id, sender))) return;
    if (splitArgs(getCommandArguments(msg.text)).length !== 0) {
      await sender.sendMessage(msg.chat.id, 'Invalid arguments');
      return;
    }
    try {
      const group = await groupRepo.getGroup(msg.chat.id);
      const headers = ['AppId', 'Score', 'Ratings'];
      const appleRows = await scoreRowsFor(group.appleApps, appleScraper);
      const googleRows = await scoreRowsFor(group.googleApps, googleScraper);

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

async function scoreRowsFor(apps, scraper) {
  const rows = [];
  for (const a of apps) {
    try {
      const resp = await scraper.getApp(a.appId, a.country);
      if (!resp) {
        rows.push([a.appId, '?', '?']);
        continue;
      }
      rows.push([a.appId, formatScore(resp.score), String(resp.ratings ?? 0)]);
    } catch {
      rows.push([a.appId, '?', '?']);
    }
  }
  return rows;
}

function renderTable(rows, headers) {
  if (rows.length === 0) return '<i>(none)</i>\n';
  return `<pre>${buildTable(headers, rows)}</pre>\n`;
}

function formatScore(score) {
  const v = Number(score);
  if (!Number.isFinite(v)) return '?';
  return (Math.round(v * 10) / 10).toFixed(1);
}
