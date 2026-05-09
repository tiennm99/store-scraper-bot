// Raw fetch wrapper for the Telegram Bot API.

const TELEGRAM_BASE = 'https://api.telegram.org';

export function createTelegramApi(token) {
  const base = `${TELEGRAM_BASE}/bot${token}`;

  async function callJson(method, payload) {
    const res = await fetch(`${base}/${method}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const text = await res.text();
    if (!res.ok) throw new Error(`telegram ${method} failed: ${res.status} ${text}`);
    return JSON.parse(text);
  }

  // multipart/form-data — for sendDocument. WHATWG FormData/Blob is native.
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
    if (!res.ok) throw new Error(`telegram ${method} failed: ${res.status} ${text}`);
    return JSON.parse(text);
  }

  return {
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
