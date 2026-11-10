/**
 * SmartPark — Frontend Application Logic
 * Supports Customer Portal (Requests) and Admin Console (ACID Approvals & Management)
 */

const API = '';   // Same origin

// ─── App State ────────────────────────────────────────────────────────────────
let state = {
  // Current user
  role:            null,       // 'customer' | 'admin'
  userId:          null,
  username:        '',
  fullName:        '',

  // Admin state
  adminView:       'dashboard',
  slots:           [],
  reservations:    [],
  requests:        [],
  filteredSlots:   [],
  filteredReservations: [],
  filteredRequests: [],
  slotFilter:      'all',
  resFilter:       'all',
  reqFilter:       'all',

  // Customer state
  custView:        'cust-slots',
  custSlots:       [],
  custRequests:    [],

  // Modal target IDs
  pendingCancelId:        null,
  pendingApproveId:       null,
  pendingRejectId:        null,
  pendingCancelRequestId: null,
  pendingReleaseSlotId:   null,
};

// ─── Utilities ────────────────────────────────────────────────────────────────
function qs(sel, ctx = document) { return ctx.querySelector(sel); }
function qsa(sel, ctx = document) { return [...ctx.querySelectorAll(sel)]; }

function escHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function showToast(msg, type = 'success') {
  const t = qs('#toast');
  if (!t) return;
  t.textContent = msg;
  t.className   = `toast toast-${type}`;
  t.classList.remove('hidden');
  clearTimeout(t._tid);
  t._tid = setTimeout(() => t.classList.add('hidden'), 3500);
}

function setLoading(btnEl, loading) {
  if (!btnEl) return;
  if (loading) { btnEl.disabled = true; btnEl.classList.add('loading'); }
  else         { btnEl.disabled = false; btnEl.classList.remove('loading'); }
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}

function getZoneName(slotName) {
  if (!slotName) return '';
  const prefix = slotName.split('-')[0];
  const zones = { A: 'Zone A', B: 'Zone B', C: 'Zone C', D: 'Zone D' };
  return zones[prefix] || prefix;
}

function closeModalOutside(event, modalId) {
  if (event.target === qs(`#${modalId}`)) {
    const modal = qs(`#${modalId}`);
    if (modal) modal.classList.add('hidden');
  }
}

// ─── Database Status ──────────────────────────────────────────────────────────
async function checkDBStatus() {
  try {
    const r = await fetch(`${API}/api/status`);
    const d = await r.json();

    // Login page badge
    const badge = qs('#login-db-status');
    if (badge) {
      const dot = badge.querySelector('.dot');
      const span = badge.querySelector('span');
      if (d.connected) {
        if (dot) dot.className = 'dot green pulse';
        if (span) span.textContent = 'MySQL · smart_parking · Connected';
      } else {
        if (dot) dot.className = 'dot orange pulse';
        if (span) span.textContent = 'DB Offline — ' + (d.message || 'Check connection');
      }
    }

    // Customer topbar badge
    const custBadge = qs('#cust-db-badge');
    if (custBadge) {
      const span = custBadge.querySelector('span');
      const dot = custBadge.querySelector('.dot');
      if (d.connected) {
        if (dot) dot.className = 'dot green pulse';
        if (span) span.textContent = 'Database: Online';
      } else {
        if (dot) dot.className = 'dot orange pulse';
        if (span) span.textContent = 'Database: Offline';
      }
    }

    // Admin topbar badge
    const topBadge = qs('#db-status-badge');
    if (topBadge) {
      const dot = topBadge.querySelector('.dot');
      const txt = qs('#db-status-text');
      if (d.connected) {
        if (dot) dot.className = 'dot green pulse';
        if (txt) txt.textContent = 'Database: Online (MySQL 3306)';
        topBadge.style.borderColor = '#a7e6c1';
      } else {
        if (dot) dot.className = 'dot orange pulse';
        if (txt) txt.textContent = 'Database: Offline';
        topBadge.style.borderColor = '#fcd3ae';
      }
    }
  } catch (_) {}
}

// ═══════════════════════════════════════════════════════════════════════════════
// AUTHENTICATION & LOGIN TABS
// ═══════════════════════════════════════════════════════════════════════════════

function switchLoginTab(tab) {
  const tabCust  = qs('#tab-customer');
  const tabAdmin = qs('#tab-admin');
  const formCust = qs('#form-customer-signin');
  const formReg  = qs('#form-customer-register');
  const formAdm  = qs('#form-admin-login');

  if (tab === 'customer') {
    if (tabCust) tabCust.classList.add('active');
    if (tabAdmin) tabAdmin.classList.remove('active');
    if (formCust) formCust.classList.remove('hidden');
    if (formReg)  formReg.classList.add('hidden');
    if (formAdm)  formAdm.classList.add('hidden');
  } else {
    if (tabAdmin) tabAdmin.classList.add('active');
    if (tabCust)  tabCust.classList.remove('active');
    if (formCust) formCust.classList.add('hidden');
    if (formReg)  formReg.classList.add('hidden');
    if (formAdm)  formAdm.classList.remove('hidden');
  }
}

function showRegisterForm() {
  const sc = qs('#form-customer-signin');
  const sr = qs('#form-customer-register');
  if (sc) sc.classList.add('hidden');
  if (sr) sr.classList.remove('hidden');
  const re = qs('#register-error');
  const rs = qs('#register-success');
  if (re) re.classList.add('hidden');
  if (rs) rs.classList.add('hidden');
}

function showSignInForm() {
  const sc = qs('#form-customer-signin');
  const sr = qs('#form-customer-register');
  if (sr) sr.classList.add('hidden');
  if (sc) sc.classList.remove('hidden');
  const cle = qs('#cust-login-error');
  if (cle) cle.classList.add('hidden');
}

function fillCustomerDemo() {
  const u = qs('#cust-username');
  const p = qs('#cust-password');
  if (u) u.value = 'ravi';
  if (p) p.value = 'pass123';
}

function fillAdminDemo() {
  const u = qs('#admin-username');
  const p = qs('#admin-password');
  if (u) u.value = 'admin';
  if (p) p.value = 'admin123';
}

// Attach login listeners when DOM is loaded or immediately if already loaded
function attachAuthListeners() {
  // Customer Sign In Handler
  const custForm = qs('#customer-login-form');
  if (custForm && !custForm._hasListener) {
    custForm._hasListener = true;
    custForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn   = qs('#cust-login-btn');
      const errEl = qs('#cust-login-error');
      const username = qs('#cust-username').value.trim();
      const password = qs('#cust-password').value;

      if (errEl) errEl.classList.add('hidden');
      setLoading(btn, true);

      try {
        const r = await fetch(`${API}/api/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password }),
        });
        const d = await r.json();

        if (d.success && d.role === 'customer') {
          state.role     = 'customer';
          state.userId   = d.userId;
          state.username = d.username;
          state.fullName = d.fullName || d.username;

          // Update customer UI
          const welcomeEl = qs('#cust-welcome-name');
          const nameEl    = qs('#cust-sb-name');
          const avatarEl  = qs('#cust-avatar');
          if (welcomeEl) welcomeEl.textContent = state.fullName;
          if (nameEl)    nameEl.textContent    = state.fullName.toUpperCase();
          if (avatarEl)  avatarEl.textContent  = state.fullName[0].toUpperCase();

          qs('#page-login').classList.remove('active');
          qs('#page-app').classList.remove('active');
          qs('#page-customer').classList.add('active');

          custSwitchView('cust-slots');
          await custRefreshAll();
          showToast(`Welcome back, ${state.fullName}!`);
        } else {
          if (errEl) {
            errEl.textContent = d.message || 'Login failed.';
            errEl.classList.remove('hidden');
          }
        }
      } catch (err) {
        if (errEl) {
          errEl.textContent = 'Cannot reach server. Ensure Node.js backend is running.';
          errEl.classList.remove('hidden');
        }
      } finally {
        setLoading(btn, false);
      }
    });
  }

  // Customer Register Handler
  const regForm = qs('#register-form');
  if (regForm && !regForm._hasListener) {
    regForm._hasListener = true;
    regForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn       = qs('#register-btn');
      const errEl     = qs('#register-error');
      const successEl = qs('#register-success');
      const fullName  = qs('#reg-fullname').value.trim();
      const username  = qs('#reg-username').value.trim();
      const password  = qs('#reg-password').value;

      if (errEl) errEl.classList.add('hidden');
      if (successEl) successEl.classList.add('hidden');
      setLoading(btn, true);

      try {
        const r = await fetch(`${API}/api/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ fullName, username, password }),
        });
        const d = await r.json();

        if (d.success) {
          if (successEl) {
            successEl.textContent = 'Account created successfully! Switching to sign in…';
            successEl.classList.remove('hidden');
          }
          setTimeout(() => {
            showSignInForm();
            qs('#cust-username').value = username;
            qs('#cust-password').value = password;
          }, 1500);
        } else {
          if (errEl) {
            errEl.textContent = d.message || 'Registration failed.';
            errEl.classList.remove('hidden');
          }
        }
      } catch (err) {
        if (errEl) {
          errEl.textContent = 'Server error during registration. Please try again.';
          errEl.classList.remove('hidden');
        }
      } finally {
        setLoading(btn, false);
      }
    });
  }

  // Admin Login Handler
  const adminForm = qs('#admin-login-form');
  if (adminForm && !adminForm._hasListener) {
    adminForm._hasListener = true;
    adminForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn   = qs('#admin-login-btn');
      const errEl = qs('#admin-login-error');
      const username = qs('#admin-username').value.trim();
      const password = qs('#admin-password').value;

      if (errEl) errEl.classList.add('hidden');
      setLoading(btn, true);

      try {
        const r = await fetch(`${API}/api/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password }),
        });
        const d = await r.json();

        if (d.success && d.role === 'admin') {
          state.role     = 'admin';
          state.username = d.username;
          state.fullName = d.fullName || 'Administrator';

          const sbUser = qs('#sb-username');
          const sbAv   = qs('#sb-avatar');
          if (sbUser) sbUser.textContent = state.username.toUpperCase();
          if (sbAv)   sbAv.textContent   = state.username[0].toUpperCase();

          qs('#page-login').classList.remove('active');
          qs('#page-customer').classList.remove('active');
          qs('#page-app').classList.add('active');

          switchView('dashboard');
          await refreshAll();
          showToast('Signed in as Administrator');
        } else {
          if (errEl) {
            errEl.textContent = d.message || 'Invalid administrator credentials.';
            errEl.classList.remove('hidden');
          }
        }
      } catch (err) {
        if (errEl) {
          errEl.textContent = 'Cannot reach server. Ensure Node.js backend is running.';
          errEl.classList.remove('hidden');
        }
      } finally {
        setLoading(btn, false);
      }
    });
  }
}

function logout() {
  if (!confirm('Are you sure you want to log out?')) return;
  state.role     = null;
  state.userId   = null;
  state.username = '';
  state.fullName = '';

  qs('#page-app').classList.remove('active');
  qs('#page-customer').classList.remove('active');
  qs('#page-login').classList.add('active');

  const cu = qs('#cust-username');
  const cp = qs('#cust-password');
  const au = qs('#admin-username');
  const ap = qs('#admin-password');
  if (cu) cu.value = '';
  if (cp) cp.value = '';
  if (au) au.value = '';
  if (ap) ap.value = '';

  checkDBStatus();
}

// ═══════════════════════════════════════════════════════════════════════════════
// CUSTOMER PORTAL LOGIC
// ═══════════════════════════════════════════════════════════════════════════════

function custSwitchView(view) {
  state.custView = view;
  const viewSlots = qs('#view-cust-slots');
  const viewReqs  = qs('#view-cust-requests');
  const navSlots  = qs('#cnav-slots');
  const navReqs   = qs('#cnav-requests');
  const title     = qs('#cust-page-title');

  if (view === 'cust-slots') {
    if (viewSlots) viewSlots.classList.add('active');
    if (viewReqs)  viewReqs.classList.remove('active');
    if (navSlots)  navSlots.classList.add('active');
    if (navReqs)   navReqs.classList.remove('active');
    if (title)     title.textContent = 'Available Parking Slots';
  } else {
    if (viewSlots) viewSlots.classList.remove('active');
    if (viewReqs)  viewReqs.classList.add('active');
    if (navSlots)  navSlots.classList.remove('active');
    if (navReqs)   navReqs.classList.add('active');
    if (title)     title.textContent = 'My Reservation Requests';
  }
}

async function custRefreshAll() {
  await checkDBStatus();
  await Promise.all([loadCustomerSlots(), loadCustomerRequests()]);
}

async function loadCustomerSlots() {
  try {
    const r = await fetch(`${API}/api/slots`);
    state.custSlots = await r.json();
    renderCustomerSlots();
  } catch (_) {}
}

function renderCustomerSlots() {
  const grid = qs('#cust-slots-grid');
  if (!grid) return;

  if (!state.custSlots.length) {
    grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🅿️</div><div class="empty-state-text">No parking slots configured.</div></div>';
    return;
  }

  grid.innerHTML = state.custSlots.map(s => `
    <div class="slot-card">
      <div class="slot-card-header">
        <div>
          <div class="slot-zone">${getZoneName(s.slot_name)}</div>
          <div class="slot-id">#${s.slot_name}</div>
        </div>
        <span class="badge ${s.is_available ? 'badge-active' : 'badge-occupied'}">
          <div class="dot ${s.is_available ? 'green' : 'orange'}"></div>
          ${s.is_available ? 'Available' : 'Occupied'}
        </span>
      </div>
      <div class="slot-name">${s.slot_name}</div>
      <div class="slot-card-action">
        ${s.is_available
          ? `<button class="btn btn-reserve-action btn-full" onclick="openRequestModal(${s.slot_id}, '${s.slot_name}')">
               Request This Slot →
             </button>`
          : `<div class="slot-occupied-label">Currently Occupied</div>`}
      </div>
    </div>
  `).join('');
}

async function loadCustomerRequests() {
  if (!state.userId) return;
  try {
    const r = await fetch(`${API}/api/requests/my/${state.userId}`);
    state.custRequests = await r.json();

    const badge = qs('#cnav-req-count');
    if (badge) badge.textContent = state.custRequests.length;

    renderCustomerRequests();
  } catch (_) {}
}

function renderCustomerRequests() {
  const tbody = qs('#cust-requests-body');
  if (!tbody) return;

  if (!state.custRequests.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-row">You haven\'t made any reservation requests yet.</td></tr>';
    return;
  }

  tbody.innerHTML = state.custRequests.map(r => {
    let statusBadge = '';
    if (r.status === 'PENDING') {
      statusBadge = '<span class="badge" style="background:#fff7ed;color:#c2410c;border-color:#fed7aa"><div class="dot orange pulse"></div> Pending Approval</span>';
    } else if (r.status === 'APPROVED') {
      statusBadge = '<span class="badge badge-active"><div class="dot green"></div> Approved</span>';
    } else {
      statusBadge = '<span class="badge badge-cancelled"><div class="dot red"></div> Rejected</span>';
    }

    const actionBtn = (r.status === 'PENDING' || r.status === 'APPROVED')
      ? `<button class="btn btn-cancel-request" onclick="openCancelRequestModal(${r.request_id}, '${escHtml(r.slot_name)}', '${r.status}')">
           ${r.status === 'PENDING' ? 'Cancel Request' : 'Cancel Booking'}
         </button>`
      : '<span style="color:var(--text-muted);font-size:0.8rem">—</span>';

    return `
      <tr>
        <td><strong>#REQ-${r.request_id}</strong></td>
        <td><span class="badge" style="background:#eff6ff;color:#1d4ed8;border-color:#bfdbfe;font-weight:700">${escHtml(r.slot_name)}</span></td>
        <td>${formatDate(r.requested_at)}</td>
        <td>${statusBadge}</td>
        <td>${r.admin_note ? escHtml(r.admin_note) : '<span style="color:var(--text-muted)">—</span>'}</td>
        <td>${r.reservation_id ? `<strong>#RES-${r.reservation_id}</strong>` : '<span style="color:var(--text-muted)">—</span>'}</td>
        <td>${actionBtn}</td>
      </tr>
    `;
  }).join('');
}

// Customer: Cancel Request Modal & Action
function openCancelRequestModal(requestId, slotName, status) {
  state.pendingCancelRequestId = requestId;
  const modal = qs('#modal-cancel-request');
  const txt   = qs('#cancel-request-text');
  const err   = qs('#cancel-request-error');

  if (err) err.classList.add('hidden');
  if (txt) {
    txt.innerHTML = `Are you sure you want to cancel your ${status === 'APPROVED' ? 'active booking' : 'request'} for <strong>Slot ${slotName}</strong> (Request #REQ-${requestId})?`;
  }
  if (modal) modal.classList.remove('hidden');
}

function closeCancelRequestModal() {
  const modal = qs('#modal-cancel-request');
  if (modal) modal.classList.add('hidden');
  state.pendingCancelRequestId = null;
}

async function confirmCancelRequest() {
  const id  = state.pendingCancelRequestId;
  const btn = qs('#cancel-request-confirm-btn');
  const err = qs('#cancel-request-error');
  if (!id) return;

  setLoading(btn, true);
  if (err) err.classList.add('hidden');

  try {
    const r = await fetch(`${API}/api/requests/${id}/cancel`, { method: 'PUT' });
    const d = await r.json();

    if (r.ok && d.success) {
      showToast('Reservation request cancelled.');
      closeCancelRequestModal();
      await custRefreshAll();
    } else {
      if (err) {
        err.textContent = d.error || 'Failed to cancel request.';
        err.classList.remove('hidden');
      }
    }
  } catch (_) {
    if (err) {
      err.textContent = 'Network error. Please try again.';
      err.classList.remove('hidden');
    }
  } finally {
    setLoading(btn, false);
  }
}

// Customer: Open Request Modal
function openRequestModal(slotId, slotName) {
  const modal = qs('#modal-request');
  const slotIdInput = qs('#request-slot-id');
  const preview = qs('#request-slot-preview');
  const errEl = qs('#request-error');
  const succEl = qs('#request-success');

  if (slotIdInput) slotIdInput.value = slotId;
  if (errEl)  errEl.classList.add('hidden');
  if (succEl) succEl.classList.add('hidden');

  if (preview) {
    preview.innerHTML = `
      <div style="display:flex;align-items:center;justify-content:space-between;padding:12px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:10px;margin-bottom:16px;">
        <div>
          <div style="font-size:0.78rem;font-weight:700;color:#166534;letter-spacing:0.05em;text-transform:uppercase">${getZoneName(slotName)}</div>
          <div style="font-size:1.25rem;font-weight:800;color:#14532d">${escHtml(slotName)}</div>
        </div>
        <span class="badge badge-active"><div class="dot green"></div> Ready to Request</span>
      </div>
      <p style="font-size:0.875rem;color:#4b5563;margin-bottom:16px;">
        Requesting on behalf of: <strong>${escHtml(state.fullName)}</strong> (@${escHtml(state.username)})
      </p>
    `;
  }

  if (modal) modal.classList.remove('hidden');
}

function closeRequestModal() {
  const modal = qs('#modal-request');
  if (modal) modal.classList.add('hidden');
}

async function submitRequest(event) {
  event.preventDefault();
  const btn = qs('#request-submit-btn');
  const slotId = qs('#request-slot-id')?.value;
  const errEl = qs('#request-error');
  const succEl = qs('#request-success');

  if (!slotId || !state.userId) return;
  setLoading(btn, true);
  if (errEl) errEl.classList.add('hidden');
  if (succEl) succEl.classList.add('hidden');

  try {
    const r = await fetch(`${API}/api/requests`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: state.userId,
        customerName: state.fullName,
        slotId: Number(slotId)
      })
    });
    const d = await r.json();

    if (r.ok && d.success) {
      if (succEl) {
        succEl.textContent = 'Request submitted successfully! Awaiting administrator approval.';
        succEl.classList.remove('hidden');
      }
      showToast('Reservation request sent to Administrator!');
      setTimeout(() => {
        closeRequestModal();
        custSwitchView('cust-requests');
        custRefreshAll();
      }, 1200);
    } else {
      if (errEl) {
        errEl.textContent = d.error || 'Failed to submit request.';
        errEl.classList.remove('hidden');
      }
    }
  } catch (err) {
    if (errEl) {
      errEl.textContent = 'Network error. Please try again.';
      errEl.classList.remove('hidden');
    }
  } finally {
    setLoading(btn, false);
  }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ADMIN CONSOLE LOGIC
// ═══════════════════════════════════════════════════════════════════════════════

const ADMIN_VIEW_LABELS = {
  dashboard:    'Dashboard Overview',
  requests:     'Pending Reservation Requests',
  slots:        'Parking Slot Management',
  reservations: 'Reservation Management',
};

function switchView(view) {
  state.adminView = view;
  qsa('#page-app .view').forEach(v   => v.classList.remove('active'));
  qsa('#page-app .nav-item').forEach(n => n.classList.remove('active'));

  const viewEl = qs(`#view-${view}`);
  const navEl  = qs(`#nav-${view}`);
  if (viewEl) viewEl.classList.add('active');
  if (navEl)  navEl.classList.add('active');

  const pageTitle = qs('#page-title');
  if (pageTitle) pageTitle.textContent = ADMIN_VIEW_LABELS[view] || view;
}

async function refreshAll() {
  await checkDBStatus();
  await Promise.all([loadStats(), loadRequests(), loadSlots(), loadReservations()]);
}

async function loadStats() {
  try {
    const r = await fetch(`${API}/api/slots/stats`);
    const d = await r.json();
    const t = qs('#stat-total');
    const a = qs('#stat-avail');
    const o = qs('#stat-occ');
    const p = qs('#stat-pending');

    if (t) t.textContent = d.total     ?? '—';
    if (a) a.textContent = d.available ?? '—';
    if (o) o.textContent = d.occupied  ?? '—';
    if (p) p.textContent = d.pending   ?? '0';
  } catch (_) {}
}

// ─── ADMIN: REQUESTS ──────────────────────────────────────────────────────────
async function loadRequests() {
  try {
    const r = await fetch(`${API}/api/requests`);
    state.requests = await r.json();

    const pendingCount = state.requests.filter(req => req.status === 'PENDING').length;
    const navPending = qs('#nav-pending-count');
    const tabPending = qs('#req-pending-count');
    if (navPending) navPending.textContent = pendingCount;
    if (tabPending) tabPending.textContent = pendingCount;

    applyRequestFilter();
  } catch (_) {}
}

function filterRequests(filter) {
  state.reqFilter = filter;
  qsa('#req-filter-tabs .filter-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.filter === filter);
  });
  applyRequestFilter();
}

function searchRequests() {
  applyRequestFilter();
}

function applyRequestFilter() {
  const search = (qs('#req-search')?.value || '').toLowerCase();
  state.filteredRequests = state.requests.filter(r => {
    const matchFilter = state.reqFilter === 'all' ? true : r.status === state.reqFilter;
    const matchSearch =
      !search ||
      r.customer_name.toLowerCase().includes(search) ||
      r.slot_name.toLowerCase().includes(search) ||
      String(r.request_id).includes(search);
    return matchFilter && matchSearch;
  });
  renderRequestsTable();
}

function renderRequestsTable() {
  const tbody = qs('#requests-body');
  if (!tbody) return;

  if (!state.filteredRequests.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-row">No reservation requests match the filter.</td></tr>';
    return;
  }

  tbody.innerHTML = state.filteredRequests.map(r => {
    let statusBadge = '';
    if (r.status === 'PENDING') {
      statusBadge = '<span class="badge" style="background:#fff7ed;color:#c2410c;border-color:#fed7aa"><div class="dot orange pulse"></div> Pending</span>';
    } else if (r.status === 'APPROVED') {
      statusBadge = '<span class="badge badge-active"><div class="dot green"></div> Approved</span>';
    } else {
      statusBadge = '<span class="badge badge-cancelled"><div class="dot red"></div> Rejected</span>';
    }

    const actions = r.status === 'PENDING'
      ? `<div style="display:flex;gap:6px">
           <button class="btn btn-primary btn-sm" onclick="openApproveModal(${r.request_id}, '${escHtml(r.customer_name)}', '${escHtml(r.slot_name)}')">
             ✓ Approve
           </button>
           <button class="btn btn-danger btn-sm" onclick="openRejectModal(${r.request_id}, '${escHtml(r.customer_name)}', '${escHtml(r.slot_name)}')">
             ✕ Reject
           </button>
         </div>`
      : `<span style="font-size:0.8rem;color:var(--text-muted)">Completed</span>`;

    return `
      <tr>
        <td><strong>#REQ-${r.request_id}</strong></td>
        <td><strong>${escHtml(r.customer_name)}</strong></td>
        <td><span class="badge" style="background:#eff6ff;color:#1d4ed8;border-color:#bfdbfe;font-weight:700">${escHtml(r.slot_name)}</span></td>
        <td>${formatDate(r.requested_at)}</td>
        <td>${statusBadge}</td>
        <td>${actions}</td>
      </tr>
    `;
  }).join('');
}

// Admin: Approve Request Modal & Flow
function openApproveModal(requestId, customerName, slotName) {
  state.pendingApproveId = requestId;
  const modal = qs('#modal-approve');
  const txt = qs('#approve-confirm-text');
  const err = qs('#approve-error');
  if (err) err.classList.add('hidden');
  if (txt) {
    txt.innerHTML = `Approve request <strong>#REQ-${requestId}</strong> for <strong>${customerName}</strong> on <strong>Slot ${slotName}</strong>?<br><small style="color:#6b7280">This executes an ACID transaction with row-level locking to reserve the slot.</small>`;
  }
  if (modal) modal.classList.remove('hidden');
}

function closeApproveModal() {
  const modal = qs('#modal-approve');
  if (modal) modal.classList.add('hidden');
  state.pendingApproveId = null;
}

async function confirmApprove() {
  const id  = state.pendingApproveId;
  const btn = qs('#approve-confirm-btn');
  const err = qs('#approve-error');
  if (!id) return;

  setLoading(btn, true);
  if (err) err.classList.add('hidden');

  try {
    const r = await fetch(`${API}/api/requests/${id}/approve`, { method: 'PUT' });
    const d = await r.json();

    if (r.ok && d.success) {
      showToast(`Request #${id} approved! Reservation #${d.reservationId} created.`);
      closeApproveModal();
      await refreshAll();
    } else {
      if (err) {
        err.textContent = d.error || 'Approval failed.';
        err.classList.remove('hidden');
      }
    }
  } catch (e) {
    if (err) {
      err.textContent = 'Network error while approving request.';
      err.classList.remove('hidden');
    }
  } finally {
    setLoading(btn, false);
  }
}

// Admin: Reject Request Modal & Flow
function openRejectModal(requestId, customerName, slotName) {
  state.pendingRejectId = requestId;
  const modal = qs('#modal-reject');
  const txt = qs('#reject-confirm-text');
  const note = qs('#reject-note');
  const err = qs('#reject-error');

  if (err) err.classList.add('hidden');
  if (note) note.value = '';
  if (txt) {
    txt.innerHTML = `Reject reservation request <strong>#REQ-${requestId}</strong> for <strong>${customerName}</strong> (${slotName})?`;
  }
  if (modal) modal.classList.remove('hidden');
}

function closeRejectModal() {
  const modal = qs('#modal-reject');
  if (modal) modal.classList.add('hidden');
  state.pendingRejectId = null;
}

async function confirmReject(event) {
  event.preventDefault();
  const id   = state.pendingRejectId;
  const btn  = qs('#reject-confirm-btn');
  const note = qs('#reject-note')?.value.trim();
  const err  = qs('#reject-error');
  if (!id) return;

  setLoading(btn, true);
  if (err) err.classList.add('hidden');

  try {
    const r = await fetch(`${API}/api/requests/${id}/reject`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ note })
    });
    const d = await r.json();

    if (r.ok && d.success) {
      showToast(`Request #${id} rejected.`);
      closeRejectModal();
      await refreshAll();
    } else {
      if (err) {
        err.textContent = d.error || 'Rejection failed.';
        err.classList.remove('hidden');
      }
    }
  } catch (e) {
    if (err) {
      err.textContent = 'Network error while rejecting.';
      err.classList.remove('hidden');
    }
  } finally {
    setLoading(btn, false);
  }
}

// ─── ADMIN: SLOTS ─────────────────────────────────────────────────────────────
async function loadSlots() {
  try {
    const r = await fetch(`${API}/api/slots`);
    state.slots = await r.json();
    state.filteredSlots = [...state.slots];

    const slotCount = qs('#nav-slot-count');
    if (slotCount) slotCount.textContent = state.slots.length;

    const avail = state.slots.filter(s => s.is_available).length;
    const occ   = state.slots.filter(s => !s.is_available).length;
    const allC   = qs('#filter-all-count');
    const availC = qs('#filter-avail-count');
    const occC   = qs('#filter-occ-count');
    if (allC)   allC.textContent   = state.slots.length;
    if (availC) availC.textContent = avail;
    if (occC)   occC.textContent   = occ;

    renderDashboardSlotGrid();
    applySlotFilter();
  } catch (_) {}
}

function renderDashboardSlotGrid() {
  const grid = qs('#dash-slot-grid');
  if (!grid) return;

  if (!state.slots.length) {
    grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🅿️</div><div class="empty-state-text">No slots found</div></div>';
    return;
  }

  grid.innerHTML = state.slots.map(s => `
    <div class="slot-mini-card ${s.is_available ? 'avail' : 'occ'}"
         onclick="${s.is_available ? `openReserveModal(${s.slot_id})` : `openReleaseSlotModal(${s.slot_id}, '${s.slot_name}')`}"
         title="${s.is_available ? `Click to direct-reserve ${s.slot_name}` : `Click to release ${s.slot_name}`}">
      <div class="slot-mini-name">${s.slot_name}</div>
      <div class="slot-mini-status">${s.is_available ? 'Free' : 'Occupied 🔓'}</div>
    </div>
  `).join('');
}

function filterSlots(filter) {
  state.slotFilter = filter;
  qsa('#slot-filter-tabs .filter-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.filter === filter);
  });
  applySlotFilter();
}

function applySlotFilter() {
  const search = (qs('#slot-search')?.value || '').toLowerCase();
  state.filteredSlots = state.slots.filter(s => {
    const matchFilter =
      state.slotFilter === 'all'       ? true :
      state.slotFilter === 'available' ? s.is_available :
      state.slotFilter === 'occupied'  ? !s.is_available : true;
    const matchSearch = !search || s.slot_name.toLowerCase().includes(search);
    return matchFilter && matchSearch;
  });
  renderSlotsGrid();
}

function searchSlots() { applySlotFilter(); }

function renderSlotsGrid() {
  const grid = qs('#slots-grid');
  if (!grid) return;

  if (!state.filteredSlots.length) {
    grid.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🔍</div><div class="empty-state-text">No slots match your filter.</div></div>';
    return;
  }

  grid.innerHTML = state.filteredSlots.map(s => `
    <div class="slot-card">
      <div class="slot-card-header">
        <div>
          <div class="slot-zone">${getZoneName(s.slot_name)}</div>
          <div class="slot-id">#${s.slot_name}</div>
        </div>
        <span class="badge ${s.is_available ? 'badge-active' : 'badge-occupied'}">
          <div class="dot ${s.is_available ? 'green' : 'orange'}"></div>
          ${s.is_available ? 'Available' : 'Occupied'}
        </span>
      </div>
      <div class="slot-name">${s.slot_name}</div>
      <div class="slot-card-action">
        ${s.is_available
          ? `<button class="btn btn-reserve-action btn-full" onclick="openReserveModal(${s.slot_id})">Direct Reserve →</button>`
          : `<button class="btn btn-warning-action btn-full" onclick="openReleaseSlotModal(${s.slot_id}, '${s.slot_name}')">Release Slot 🔓</button>`}
      </div>
    </div>
  `).join('');
}

// ─── ADMIN: RESERVATIONS ──────────────────────────────────────────────────────
async function loadReservations() {
  try {
    const r = await fetch(`${API}/api/reservations`);
    state.reservations = await r.json();

    const activeCount = state.reservations.filter(r => r.status === 'ACTIVE').length;
    const navCount = qs('#nav-res-count');
    if (navCount) navCount.textContent = activeCount;

    renderRecentReservations();
    applyResFilter();
  } catch (_) {}
}

function renderRecentReservations() {
  const tbody = qs('#recent-res-body');
  if (!tbody) return;

  const recent = state.reservations.slice(0, 6);
  if (!recent.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty-row">No reservations yet.</td></tr>';
    return;
  }

  tbody.innerHTML = recent.map(r => `
    <tr>
      <td><strong>#${r.reservation_id}</strong></td>
      <td>${escHtml(r.customer_name)}</td>
      <td>${escHtml(r.slot_name)}</td>
      <td>${formatDate(r.reservation_date)}</td>
      <td><span class="badge ${r.status === 'ACTIVE' ? 'badge-active' : 'badge-cancelled'}">${r.status}</span></td>
    </tr>
  `).join('');
}

function filterReservations(filter) {
  state.resFilter = filter;
  qsa('#view-reservations .filter-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.filter === filter);
  });
  applyResFilter();
}

function searchReservations() { applyResFilter(); }

function applyResFilter() {
  const search = (qs('#res-search')?.value || '').toLowerCase();
  state.filteredReservations = state.reservations.filter(r => {
    const matchFilter = state.resFilter === 'all' ? true : r.status === state.resFilter;
    const matchSearch =
      !search ||
      r.customer_name.toLowerCase().includes(search) ||
      r.slot_name.toLowerCase().includes(search) ||
      String(r.reservation_id).includes(search);
    return matchFilter && matchSearch;
  });
  renderReservationsTable();
}

function renderReservationsTable() {
  const tbody = qs('#reservations-body');
  if (!tbody) return;

  if (!state.filteredReservations.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-row">No reservations match your filter.</td></tr>';
    return;
  }

  tbody.innerHTML = state.filteredReservations.map(r => `
    <tr>
      <td><strong>#${r.reservation_id}</strong></td>
      <td><strong>${escHtml(r.customer_name)}</strong></td>
      <td>
        <span class="badge" style="background:#eff6ff;color:#1d4ed8;border-color:#bfdbfe;font-weight:700">
          ${escHtml(r.slot_name)}
        </span>
      </td>
      <td>${formatDate(r.reservation_date)}</td>
      <td><span class="badge ${r.status === 'ACTIVE' ? 'badge-active' : 'badge-cancelled'}">${r.status}</span></td>
      <td>
        ${r.status === 'ACTIVE'
          ? `<button class="btn btn-cancel-action" onclick="openCancelModal(${r.reservation_id})">Cancel</button>`
          : '<span style="color:var(--text-muted);font-size:0.78rem">—</span>'}
      </td>
    </tr>
  `).join('');
}

// Admin: Direct Reserve Modal
async function openReserveModal(preselectedSlotId = null) {
  const modal = qs('#modal-reserve');
  const errEl = qs('#reserve-error');
  const succEl = qs('#reserve-success');
  const custInp = qs('#res-customer');

  if (errEl) errEl.classList.add('hidden');
  if (succEl) succEl.classList.add('hidden');
  if (custInp) custInp.value = '';
  if (modal) modal.classList.remove('hidden');

  const select = qs('#res-slot');
  if (select) {
    select.innerHTML = '<option value="">Loading available slots…</option>';
    try {
      const r = await fetch(`${API}/api/slots/available`);
      const slots = await r.json();
      if (!slots.length) {
        select.innerHTML = '<option value="">No slots available</option>';
      } else {
        select.innerHTML = '<option value="">— Select a slot —</option>' +
          slots.map(s => `<option value="${s.slot_id}" ${Number(preselectedSlotId) === s.slot_id ? 'selected' : ''}>${s.slot_name} (${getZoneName(s.slot_name)})</option>`).join('');
      }
    } catch (_) {
      select.innerHTML = '<option value="">Error loading slots</option>';
    }
  }
}

function closeReserveModal() {
  const modal = qs('#modal-reserve');
  if (modal) modal.classList.add('hidden');
}

async function submitReservation(e) {
  e.preventDefault();
  const btn          = qs('#reserve-submit-btn');
  const errEl        = qs('#reserve-error');
  const successEl    = qs('#reserve-success');
  const customerName = qs('#res-customer').value.trim();
  const slotId       = qs('#res-slot').value;

  if (errEl) errEl.classList.add('hidden');
  if (successEl) successEl.classList.add('hidden');

  if (!customerName) {
    if (errEl) { errEl.textContent = 'Customer name cannot be empty.'; errEl.classList.remove('hidden'); }
    return;
  }
  if (!slotId) {
    if (errEl) { errEl.textContent = 'Please select a parking slot.'; errEl.classList.remove('hidden'); }
    return;
  }

  setLoading(btn, true);
  try {
    const r = await fetch(`${API}/api/slots/reserve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ customerName, slotId: Number(slotId) }),
    });
    const d = await r.json();

    if (r.ok && d.success) {
      if (successEl) {
        successEl.textContent = `Reservation #${d.reservationId} created successfully!`;
        successEl.classList.remove('hidden');
      }
      showToast(`Reservation #${d.reservationId} created for ${customerName}!`);
      await refreshAll();
      setTimeout(closeReserveModal, 1200);
    } else {
      if (errEl) {
        errEl.textContent = d.error || 'Reservation failed.';
        errEl.classList.remove('hidden');
      }
    }
  } catch (err) {
    if (errEl) {
      errEl.textContent = 'Network error. Please try again.';
      errEl.classList.remove('hidden');
    }
  } finally {
    setLoading(btn, false);
  }
}

// Admin: Cancel Reservation Modal
function openCancelModal(reservationId) {
  state.pendingCancelId = reservationId;
  const res = state.reservations.find(r => r.reservation_id === reservationId);
  const textEl = qs('#cancel-confirm-text');
  if (textEl) {
    textEl.innerHTML =
      `Are you sure you want to cancel Reservation <strong>#${reservationId}</strong>` +
      (res ? ` for <strong>${escHtml(res.customer_name)}</strong> (Slot ${escHtml(res.slot_name)})` : '') +
      `? The parking slot will be freed immediately.`;
  }
  const err = qs('#cancel-error');
  if (err) err.classList.add('hidden');
  const modal = qs('#modal-cancel');
  if (modal) modal.classList.remove('hidden');
}

function closeCancelModal() {
  const modal = qs('#modal-cancel');
  if (modal) modal.classList.add('hidden');
  state.pendingCancelId = null;
}

async function confirmCancel() {
  const id  = state.pendingCancelId;
  const btn = qs('#cancel-confirm-btn');
  const err = qs('#cancel-error');
  if (!id) return;

  setLoading(btn, true);
  if (err) err.classList.add('hidden');

  try {
    const r = await fetch(`${API}/api/reservations/${id}`, { method: 'DELETE' });
    const d = await r.json();

    if (r.ok && d.success) {
      showToast(`Reservation #${id} cancelled. Slot is now available.`);
      closeCancelModal();
      await refreshAll();
    } else {
      if (err) {
        err.textContent = d.error || 'Cancellation failed.';
        err.classList.remove('hidden');
      }
    }
  } catch (_) {
    if (err) {
      err.textContent = 'Network error. Please try again.';
      err.classList.remove('hidden');
    }
  } finally {
    setLoading(btn, false);
  }
}

// Admin: Release Occupied Slot Modal & Action
function openReleaseSlotModal(slotId, slotName) {
  state.pendingReleaseSlotId = slotId;
  const modal = qs('#modal-release-slot');
  const txt   = qs('#release-slot-text');
  const err   = qs('#release-slot-error');

  if (err) err.classList.add('hidden');
  if (txt) {
    txt.innerHTML = `Release <strong>Slot ${slotName}</strong>? Any active booking will be cancelled and the slot will immediately become available.`;
  }
  if (modal) modal.classList.remove('hidden');
}

function closeReleaseSlotModal() {
  const modal = qs('#modal-release-slot');
  if (modal) modal.classList.add('hidden');
  state.pendingReleaseSlotId = null;
}

async function confirmReleaseSlot() {
  const id  = state.pendingReleaseSlotId;
  const btn = qs('#release-slot-confirm-btn');
  const err = qs('#release-slot-error');
  if (!id) return;

  setLoading(btn, true);
  if (err) err.classList.add('hidden');

  try {
    const r = await fetch(`${API}/api/slots/${id}/release`, { method: 'PUT' });
    const d = await r.json();

    if (r.ok && d.success) {
      showToast(d.message || `Slot #${id} released successfully.`);
      closeReleaseSlotModal();
      await refreshAll();
    } else {
      if (err) {
        err.textContent = d.error || 'Failed to release slot.';
        err.classList.remove('hidden');
      }
    }
  } catch (_) {
    if (err) {
      err.textContent = 'Network error while releasing slot.';
      err.classList.remove('hidden');
    }
  } finally {
    setLoading(btn, false);
  }
}

// ═══════════════════════════════════════════════════════════════════════════════
// INITIALIZATION
// ═══════════════════════════════════════════════════════════════════════════════

// Attach form listeners immediately or on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', attachAuthListeners);
} else {
  attachAuthListeners();
}

checkDBStatus();
setInterval(checkDBStatus, 30_000);

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closeRequestModal();
    closeApproveModal();
    closeRejectModal();
    closeReserveModal();
    closeCancelModal();
    closeCancelRequestModal();
    closeReleaseSlotModal();
  }
});
