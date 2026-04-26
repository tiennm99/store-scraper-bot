// Mirrors Java bot/table/Table.java:
//   - left-aligned columns padded to max(header, cell) width
//   - "│" column separator
//   - row separator inserted every 5 rows using "─" cells joined by "─┼─"
// Output is intended to be wrapped in <pre> for Telegram HTML rendering.
export function buildTable(headers, rows) {
  const widths = computeWidths(headers, rows);
  const parts = [];
  parts.push(writeRow(headers, widths));
  parts.push('\n');
  parts.push(writeSeparator(widths));
  for (let i = 0; i < rows.length; i++) {
    parts.push('\n');
    if (i > 0 && i % 5 === 0) {
      parts.push(writeSeparator(widths));
      parts.push('\n');
    }
    parts.push(writeRow(rows[i], widths));
  }
  return parts.join('');
}

function computeWidths(headers, rows) {
  const widths = headers.map((h) => h.length);
  for (const row of rows) {
    for (let i = 0; i < row.length && i < widths.length; i++) {
      if (row[i].length > widths[i]) widths[i] = row[i].length;
    }
  }
  return widths;
}

function writeRow(cells, widths) {
  const out = [];
  for (let i = 0; i < widths.length; i++) {
    out.push(padRight(cells[i] ?? '', widths[i]));
    if (i < widths.length - 1) out.push(' │ ');
  }
  return out.join('');
}

function writeSeparator(widths) {
  const out = [];
  for (let i = 0; i < widths.length; i++) {
    out.push('─'.repeat(widths[i]));
    if (i < widths.length - 1) out.push('─┼─');
  }
  return out.join('');
}

function padRight(s, len) {
  return s.length >= len ? s : s + ' '.repeat(len - s.length);
}

export function truncateString(s, maxLen) {
  if (s.length <= maxLen) return s;
  if (maxLen <= 3) return s.slice(0, maxLen);
  return s.slice(0, maxLen - 3) + '...';
}

export function formatNumber(n) {
  const v = Number(n);
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`;
  if (v >= 1_000) return `${(v / 1_000).toFixed(1)}K`;
  return String(v);
}
