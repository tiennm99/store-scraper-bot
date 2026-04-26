// Raw fetch wrapper for the Telegram Bot API. Replaces node-telegram-bot-api
// (which uses Node-only request/streams and bloats the Worker bundle).

const TELEGRAM_BASE = 'https://api.telegram.org';

export class TelegramApiError extends Error {
  constructor(method, status, body) {
    super(`telegram ${method} failed: ${status} ${body}`);
    this.name = 'TelegramApiError';
    this.method = method;
    this.status = status;
    this.body = body;
  }
}

export function createTelegramApi(token) {
  const base = `${TELEGRAM_BASE}/bot${token}`;

  async function callJson(method, payload) {
    const res = await fetch(`${base}/${method}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const text = await res.text();
    if (!res.ok) throw new TelegramApiError(method, res.status, text);
    return JSON.parse(text);
  }

  // multipart/form-data — for sendDocument. WHATWG FormData/Blob is native to
  // Workers; no `form-data` npm dep needed.
  async function callMultipart(method, fields, file) {
    const form = new FormData();
    for (const [k, v] of Object.entries(fields)) form.set(k, String(v));
    if (file) {
      form.set(
        file.field,
        new Blob([file.body], { type: file.contentType }),
        file.filename,
      );
    }
    const res = await fetch(`${base}/${method}`, { method: 'POST', body: form });
    const text = await res.text();
    if (!res.ok) throw new TelegramApiError(method, res.status, text);
    return JSON.parse(text);
  }

  return {
    getMe: () => callJson('getMe', {}),
    sendMessage: (chatId, text, opts = {}) =>
      callJson('sendMessage', { chat_id: chatId, text, ...opts }),
    sendDocument: (chatId, filename, body, opts = {}) =>
      callMultipart(
        'sendDocument',
        { chat_id: chatId, ...opts },
        { field: 'document', filename, body, contentType: 'application/json' },
      ),
  };
}
