const URLS = [
  "http://192.168.1.37:9790/api_public.php",
  "http://100.102.13.11:9790/api_public.php",
];

const AVATAR_COLORS = [
  "#6366f1", "#10b981", "#f59e0b", "#3b82f6", "#8b5cf6", "#ec4899", "#06b6d4"
];

const listEl = document.getElementById("list");
const summaryEl = document.getElementById("summary");
const skeletonEl = document.getElementById("skeleton");
const emptyStateEl = document.getElementById("emptyState");
const errorEl = document.getElementById("error");
const searchInput = document.getElementById("searchInput");
const clearSearchBtn = document.getElementById("clearSearch");
const fabRefresh = document.getElementById("fabRefresh");
const statusPulse = document.getElementById("statusPulse");
const installBtn = document.getElementById("installPrompt");

let deferredPrompt = null;
window.addEventListener("beforeinstallprompt", (e) => {
  e.preventDefault();
  deferredPrompt = e;
  installBtn.classList.remove("hidden");
});
installBtn.addEventListener("click", async () => {
  if (!deferredPrompt) return;
  deferredPrompt.prompt();
  await deferredPrompt.userChoice;
  deferredPrompt = null;
  installBtn.classList.add("hidden");
});

let allRows = [];

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function getInitials(name) {
  const parts = String(name || "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

function getAvatarColor(name) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
}

async function fetchData(index) {
  show(skeletonEl);
  hide(errorEl);
  hide(emptyStateEl);
  fabRefresh.classList.add("spinning");

  if (index >= URLS.length) {
    hide(skeletonEl);
    fabRefresh.classList.remove("spinning");
    summaryEl.textContent = "Gagal terhubung ke server";
    if (statusPulse) statusPulse.classList.add("warning");
    show(errorEl, "Tidak dapat terhubung ke server absensi. Silakan periksa koneksi jaringan Anda.");
    return;
  }

  try {
    const res = await fetch(URLS[index]);
    if (!res.ok) throw new Error("HTTP " + res.status);
    const json = await res.json();
    renderData(json);
  } catch (e) {
    fetchData(index + 1);
  } finally {
    fabRefresh.classList.remove("spinning");
  }
}

function renderData(json) {
  hide(skeletonEl);

  const allPresent = !!json.all_present;
  const total = json.total_not_absen || 0;
  const date = json.date || "-";
  const time = json.time || "-";

  if (statusPulse) {
    if (allPresent) {
      statusPulse.classList.remove("warning");
    } else {
      statusPulse.classList.add("warning");
    }
  }

  summaryEl.textContent = allPresent
    ? `Semua sudah absen · ${date} ${time}`
    : `${total} karyawan belum absen · ${date} ${time}`;

  allRows = [];
  const departments = json.departments || [];
  for (const dept of departments) {
    const deptName = dept.department || "-";
    allRows.push({ type: "header", name: deptName });
    for (const emp of dept.employees || []) {
      allRows.push({ type: "employee", name: emp.name || "-", dept: deptName });
    }
  }
  renderList(searchInput.value);
}

function renderList(query) {
  const q = (query || "").trim();

  if (clearSearchBtn) {
    if (q.length > 0) hide(clearSearchBtn, false); else hide(clearSearchBtn, true);
  }

  let rows = allRows;

  if (q !== "") {
    const filtered = [];
    let lastHeader = null;
    for (const row of rows) {
      if (row.type === "header") {
        lastHeader = row;
      } else if (row.name.toLowerCase().includes(q.toLowerCase())) {
        if (lastHeader && (filtered.length === 0 || filtered[filtered.length - 1] !== lastHeader)) {
          filtered.push(lastHeader);
        }
        filtered.push(row);
      }
    }
    rows = filtered;
  }

  if (rows.length === 0) {
    listEl.innerHTML = "";
    show(emptyStateEl);
    return;
  }

  hide(emptyStateEl);
  let html = "";
  for (const row of rows) {
    if (row.type === "header") {
      html += `<div class="dept-header">${escapeHtml(row.name)}</div>`;
    } else {
      const initials = getInitials(row.name);
      const bg = getAvatarColor(row.name);
      html += `
        <div class="employee-card">
          <div class="emp-main">
            <div class="emp-avatar" style="background: ${bg}">${escapeHtml(initials)}</div>
            <div class="emp-info">
              <span class="emp-name">${escapeHtml(row.name)}</span>
              <span class="emp-dept">${escapeHtml(row.dept)}</span>
            </div>
          </div>
          <span class="badge">Belum Absen</span>
        </div>`;
    }
  }
  listEl.innerHTML = html;
}

function show(el, msg) { if (msg !== undefined) el.textContent = msg; el.classList.remove("hidden"); }
function hide(el, shouldHide = true) { if (shouldHide) el.classList.add("hidden"); else el.classList.remove("hidden"); }

searchInput.addEventListener("input", () => renderList(searchInput.value));
if (clearSearchBtn) {
  clearSearchBtn.addEventListener("click", () => {
    searchInput.value = "";
    renderList("");
    searchInput.focus();
  });
}
fabRefresh.addEventListener("click", () => fetchData(0));

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("./sw.js").catch(() => {});
}

fetchData(0);
