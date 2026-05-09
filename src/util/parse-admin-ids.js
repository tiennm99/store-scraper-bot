// Parses ADMIN_IDS env (comma-separated Telegram user IDs).
// Shared between config loading and the register-webhook script.
export function parseAdminIds(raw) {
  return (raw ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    .map((s) => Number.parseInt(s, 10))
    .filter((n) => Number.isFinite(n));
}
