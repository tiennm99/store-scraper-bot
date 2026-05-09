// Resolves per-group setting overrides to effective values, falling back to
// global config defaults when a group has no override (or stored a bad value).

export function resolveDaysWarning(group, config) {
  const v = group?.settings?.numDaysWarningNotUpdated;
  return Number.isFinite(v) && v > 0 ? v : config.numDaysWarningNotUpdated;
}
