const TOKEN_KEY = "auth_token";
const PENDING_EMAIL_KEY = "pending_email";

function setToken(t) { localStorage.setItem(TOKEN_KEY, t); }
function getToken() { return localStorage.getItem(TOKEN_KEY); }
function clearToken() { localStorage.removeItem(TOKEN_KEY); }

function setPendingEmail(e) { localStorage.setItem(PENDING_EMAIL_KEY, e); }
function getPendingEmail() { return localStorage.getItem(PENDING_EMAIL_KEY); }
function clearPendingEmail() { localStorage.removeItem(PENDING_EMAIL_KEY); }

function showMsg(el, text, type = "error") {
  el.className = "msg " + type;
  el.textContent = text;
  el.style.display = "block";
}

async function apiPost(path, body) {
  const res = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || data.error || ("HTTP " + res.status));
  return data;
}

async function apiGet(path, auth = false) {
  const headers = {};
  if (auth) {
    const t = getToken();
    if (!t) throw new Error("Not logged in");
    headers["Authorization"] = "Bearer " + t;
  }
  const res = await fetch(path, { headers });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || data.error || ("HTTP " + res.status));
  return data;
}
