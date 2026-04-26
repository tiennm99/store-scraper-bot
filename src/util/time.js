// Format date as YYYY-MM-DD in given IANA timezone.
export function formatDateInTz(date, timezone) {
  const fmt = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  return fmt.format(date);
}

// Format datetime as "YYYY-MM-DD HH:MM" in given IANA timezone.
export function formatDateTimeInTz(date, timezone) {
  const fmt = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  // en-CA produces "YYYY-MM-DD, HH:MM" — strip the comma.
  return fmt.format(date).replace(', ', ' ');
}

// Day-of-week (0=Sun..6=Sat) for `date` in given timezone.
export function weekdayInTz(date, timezone) {
  const fmt = new Intl.DateTimeFormat('en-US', { timeZone: timezone, weekday: 'short' });
  const map = { Sun: 0, Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6 };
  return map[fmt.format(date)];
}

export function daysBetween(fromMs, toMs) {
  return Math.floor((toMs - fromMs) / (24 * 60 * 60 * 1000));
}
