/**
 * CleanHub UI module — status badges, order table, global search.
 * No external libraries (vanilla JS).
 */
const CleanHubUI = (() => {
  const STATUS_RULES = [
    { keys: ["menunggu", "pending", "baru"], cls: "badge-pending", label: "Pending" },
    { keys: ["cuci", "washing", "mencuci", "proses", "diproses"], cls: "badge-washing", label: "Washing" },
    { keys: ["setrika", "iron", "menyetrika"], cls: "badge-ironing", label: "Ironing" },
    { keys: ["selesai", "ready", "complete", "siap", "done"], cls: "badge-ready", label: "Ready" },
    { keys: ["batal", "cancel"], cls: "badge-cancel", label: "Cancelled" }
  ];

  function normalizeStatus(status) {
    return (status || "").toLowerCase().trim();
  }

  function statusBadge(status) {
    const s = normalizeStatus(status);
    for (const rule of STATUS_RULES) {
      if (rule.keys.some(k => s.includes(k))) {
        return `<span class="status-badge ${rule.cls}" title="${escapeHtml(status)}">${escapeHtml(status)}</span>`;
      }
    }
    return `<span class="status-badge badge-default" title="${escapeHtml(status)}">${escapeHtml(status || "-")}</span>`;
  }

  function escapeHtml(text) {
    return String(text ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function formatRupiah(amount) {
    return new Intl.NumberFormat("id-ID", {
      style: "currency",
      currency: "IDR",
      minimumFractionDigits: 0
    }).format(amount);
  }

  function isCompletedPaidOrder(status) {
    const s = normalizeStatus(status);
    return (s.includes("selesai") || s.includes("ready") || s.includes("siap")
        || s.includes("complete") || s.includes("done"))
      && !s.includes("batal") && !s.includes("cancel");
  }

  function computeAccountBalance(orders) {
    if (!orders || orders.length === 0) {
      return 0;
    }
    return orders
      .filter(r => isCompletedPaidOrder(r.status))
      .reduce((sum, r) => sum + (Number(r.totalPrice) || 0), 0);
  }

  /**
   * Render modern order table.
   * @param {object} opts
   * @param {Array} opts.orders
   * @param {string} opts.role - staff | admin | search
   * @param {function} [opts.onUpdate]
   * @param {function} [opts.onDelete]
   * @param {function} [opts.onTrack]
   */
  function renderOrderTable(opts) {
    const { orders, role, onUpdate, onDelete, onTrack } = opts;
    if (!orders || orders.length === 0) {
      return `<div class="empty-state">Tidak ada pesanan ditemukan.</div>`;
    }

    const showCustomerDetail = role === "admin" || role === "search";
    const showCustomerName = role === "staff";
    const showActions = role === "staff" || role === "admin";
    const showComplaint = role === "admin";

    let html = `<div class="table-scroll"><table class="data-table">
      <thead><tr>
        <th>ID Pesanan</th>
        ${showCustomerName ? "<th>Nama Pemesan</th>" : ""}
        ${showCustomerDetail ? "<th>Customer</th>" : ""}
        <th>Layanan</th>
        <th>Status</th>
        <th>Total</th>
        ${showComplaint ? "<th>Keluhan</th>" : ""}
        ${showActions ? "<th>Aksi</th>" : ""}
      </tr></thead><tbody>`;

    orders.forEach(r => {
      const services = (r.services || []).join(", ") || "-";
      html += `<tr class="data-row" data-order-id="${escapeHtml(r.orderId)}">
        <td><strong>${escapeHtml(r.orderId)}</strong></td>
        ${showCustomerName ? `<td>${escapeHtml(r.customerName || "-")}</td>` : ""}
        ${showCustomerDetail ? `<td><span class="cell-muted">${escapeHtml(r.customerName || "-")}</span><br><small>${escapeHtml(r.customerId || "")}</small></td>` : ""}
        <td class="cell-services">${escapeHtml(services)}</td>
        <td>${statusBadge(r.status)}</td>
        <td>${formatRupiah(r.totalPrice)}</td>
        ${showComplaint ? `<td>${escapeHtml(r.complaint && r.complaint !== "-" ? r.complaint : "-")}</td>` : ""}
        ${showActions ? `<td class="cell-actions">
          ${role === "staff" ? `<button type="button" class="btn-secondary btn-sm btn-table-update" data-id="${escapeHtml(r.orderId)}">Update</button>` : ""}
          ${onTrack ? `<button type="button" class="btn-ghost btn-sm btn-table-track" data-id="${escapeHtml(r.orderId)}">Track</button>` : ""}
          ${role === "admin" && onDelete ? `<button type="button" class="btn-danger btn-sm btn-table-delete" data-id="${escapeHtml(r.orderId)}">Hapus Riwayat</button>` : ""}
        </td>` : ""}
      </tr>`;
    });

    html += "</tbody></table></div>";
    return html;
  }

  function bindOrderTable(container, { onUpdate, onDelete, onTrack }) {
    container.querySelectorAll(".btn-table-update").forEach(btn => {
      btn.addEventListener("click", () => onUpdate && onUpdate(btn.dataset.id));
    });
    container.querySelectorAll(".btn-table-delete").forEach(btn => {
      btn.addEventListener("click", () => onDelete && onDelete(btn.dataset.id));
    });
    container.querySelectorAll(".btn-table-track").forEach(btn => {
      btn.addEventListener("click", () => onTrack && onTrack(btn.dataset.id));
    });
  }

  function renderTrackingCard(report) {
    if (!report) return "";
    const steps = ["Menunggu Diproses", "Sedang Diproses / Mencuci", "Setrika", "Selesai"];
    const current = normalizeStatus(report.status);
    let activeIdx = 0;
    if (current.includes("setrika") || current.includes("iron")) activeIdx = 2;
    else if (current.includes("selesai") || current.includes("ready") || current.includes("siap")) activeIdx = 3;
    else if (current.includes("proses") || current.includes("cuci") || current.includes("wash")) activeIdx = 1;

    const timeline = steps.map((label, i) => {
      const cls = i < activeIdx ? "done" : i === activeIdx ? "active" : "";
      return `<div class="track-step ${cls}"><span class="track-dot"></span><span>${label}</span></div>`;
    }).join("");

    return `<div class="track-card">
      <div class="track-header">
        <div><strong>${escapeHtml(report.orderId)}</strong></div>
        ${statusBadge(report.status)}
      </div>
      <div class="track-timeline">${timeline}</div>
      <div class="track-meta">
        <div><span class="label">Customer</span> ${escapeHtml(report.customerName || "-")} (${escapeHtml(report.customerId || "-")})</div>
        <div><span class="label">Layanan</span> ${escapeHtml((report.services || []).join(", "))}</div>
        <div><span class="label">Total</span> ${formatRupiah(report.totalPrice)}</div>
        ${report.complaint && report.complaint !== "-" ? `<div><span class="label">Keluhan</span> ${escapeHtml(report.complaint)}</div>` : ""}
      </div>
    </div>`;
  }

  return {
    statusBadge,
    renderOrderTable,
    bindOrderTable,
    renderTrackingCard,
    formatRupiah,
    computeAccountBalance,
    escapeHtml
  };
})();
