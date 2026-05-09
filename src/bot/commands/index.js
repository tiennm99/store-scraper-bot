// Single source of truth for bot commands.
// Adding/removing/renaming a command here automatically updates dispatch (bot.js).
// Telegram menu only updates after `npm run register` (or `npm run deploy`).
//
// Catalog entry shape:
//   { name, description, adminOnly, build(ctx) -> handler }
// `adminOnly: true` => hidden from the default Telegram menu, shown only in
// per-admin chat scope (see scripts/register-webhook.js).

import { createInfoCommand } from './info.js';
import { createAddGroupCommand } from './add-group.js';
import { createDeleteGroupCommand } from './delete-group.js';
import { createListGroupCommand } from './list-group.js';
import { createAddAppleAppCommand } from './add-apple-app.js';
import { createDeleteAppleAppCommand } from './delete-apple-app.js';
import { createAddGoogleAppCommand } from './add-google-app.js';
import { createDeleteGoogleAppCommand } from './delete-google-app.js';
import { createListAppCommand } from './list-app.js';
import { createCheckAppCommand } from './check-app.js';
import { createCheckAppScoresCommand } from './check-app-scores.js';
import { createRawAppleAppCommand } from './raw-apple-app.js';
import { createRawGoogleAppCommand } from './raw-google-app.js';
import { createGetSettingsCommand } from './get-settings.js';
import { createSetDaysWarningCommand } from './set-days-warning.js';

export const COMMAND_CATALOG = [
  { name: 'info',           description: 'Show this group ID',                       adminOnly: false, build: () => createInfoCommand() },
  { name: 'addgroup',       description: '[admin] Authorize a group',                 adminOnly: true,  build: (c) => createAddGroupCommand(c.config, c.store) },
  { name: 'delgroup',       description: '[admin] Deauthorize a group',               adminOnly: true,  build: (c) => createDeleteGroupCommand(c.config, c.store) },
  { name: 'listgroup',      description: '[admin] List authorized groups',            adminOnly: true,  build: (c) => createListGroupCommand(c.config, c.store) },
  { name: 'addapple',       description: 'Track an Apple App Store app',              adminOnly: false, build: (c) => createAddAppleAppCommand(c.store, c.appleScraper) },
  { name: 'delapple',       description: 'Stop tracking an Apple app',                adminOnly: false, build: (c) => createDeleteAppleAppCommand(c.store) },
  { name: 'addgoogle',      description: 'Track a Google Play app',                   adminOnly: false, build: (c) => createAddGoogleAppCommand(c.store, c.googleScraper) },
  { name: 'delgoogle',      description: 'Stop tracking a Google app',                adminOnly: false, build: (c) => createDeleteGoogleAppCommand(c.store) },
  { name: 'listapp',        description: 'List tracked apps in this group',           adminOnly: false, build: (c) => createListAppCommand(c.store) },
  { name: 'checkapp',       description: 'Check update status of tracked apps',       adminOnly: false, build: (c) => createCheckAppCommand(c.config, c.store, c.appleScraper, c.googleScraper) },
  { name: 'checkappscore',  description: 'Check scores + ratings of tracked apps',    adminOnly: false, build: (c) => createCheckAppScoresCommand(c.store, c.appleScraper, c.googleScraper) },
  { name: 'rawappleapp',    description: 'Dump raw Apple API JSON for an app',        adminOnly: false, build: (c) => createRawAppleAppCommand(c.store, c.appleScraper) },
  { name: 'rawgoogleapp',   description: 'Dump raw Google API JSON for an app',       adminOnly: false, build: (c) => createRawGoogleAppCommand(c.store, c.googleScraper) },
  { name: 'settings',       description: "Show this group's settings",                adminOnly: false, build: (c) => createGetSettingsCommand(c.config, c.store) },
  { name: 'setdayswarning', description: 'Set warning threshold (days, 0 = default)', adminOnly: false, build: (c) => createSetDaysWarningCommand(c.config, c.store) },
];

// Telegram menu projections.
// Default scope: user commands only — keeps admin entries out of every group's menu.
// Admin chat scope: full set including admin commands.
export const TELEGRAM_USER_COMMANDS = COMMAND_CATALOG
  .filter((e) => !e.adminOnly)
  .map(({ name, description }) => ({ command: name, description }));

export const TELEGRAM_ADMIN_COMMANDS = COMMAND_CATALOG
  .map(({ name, description }) => ({ command: name, description }));
