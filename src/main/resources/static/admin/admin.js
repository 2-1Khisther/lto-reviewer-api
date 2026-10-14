// LTO Reviewer Admin Panel
// Plain JavaScript, no libraries. Talks to the Spring Boot API on the same server (/api/...).

(function () {
  'use strict';

  const TOKEN_KEY = 'lto_admin_token';
  const NAME_KEY = 'lto_admin_name';

  const LICENSES = { NON_PROF: 'Non-Professional', PROF: 'Professional' };
  const VEHICLES = { MOTORCYCLE: 'Motorcycle / Tricycle', LIGHT: 'Light Vehicle', HEAVY: 'Heavy (future)', BUS: 'Bus (future)' };
  const SIGN_CATEGORIES = {
    REGULATORY: 'Regulatory', WARNING: 'Warning', GUIDE: 'Guide / Informative', EXPRESSWAY: 'Expressway',
    INSTRUCTION: 'Traffic Instruction', HAZARD: 'Hazard Marker', ROADWORK: 'Roadwork'
  };
  const LETTERS = ['A', 'B', 'C', 'D'];
  const VIEWS = {
    dashboard: { title: 'Dashboard', render: renderDashboard },
    questions: { title: 'Questions', render: renderQuestions },
    'road-signs': { title: 'Road Signs', render: renderRoadSigns },
    violations: { title: 'Fines & Penalties', render: renderViolations },
    users: { title: 'Users', render: renderUsers }
  };

  // ---------- small helpers ----------
  const $ = (sel) => document.querySelector(sel);

  function esc(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function store(key, value) {
    try {
      if (value == null) sessionStorage.removeItem(key); else sessionStorage.setItem(key, value);
    } catch (e) { /* storage blocked: stay logged in for this page only */ }
  }
  function load(key) {
    try { return sessionStorage.getItem(key); } catch (e) { return null; }
  }

  let token = load(TOKEN_KEY);
  let domainsCache = null;

  function peso(n) {
    return '₱' + Number(n).toLocaleString('en-PH', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
  }
  function ordinal(n) {
    return n === 4 ? '4th+' : n + (n === 1 ? 'st' : n === 2 ? 'nd' : 'rd');
  }
  function options(map, selected) {
    return Object.entries(map).map(([value, label]) =>
      `<option value="${esc(value)}"${value === selected ? ' selected' : ''}>${esc(label)}</option>`).join('');
  }

  let toastTimer;
  function toast(message, isError) {
    const t = $('#toast');
    t.textContent = message;
    t.classList.toggle('error', !!isError);
    t.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.hidden = true; }, 3000);
  }

  // ---------- API ----------
  async function api(path, options) {
    const opts = options || {};
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = 'Bearer ' + token;

    const res = await fetch('/api' + path, {
      method: opts.method || 'GET',
      headers: headers,
      body: opts.body ? JSON.stringify(opts.body) : undefined
    });

    if ((res.status === 401 || res.status === 403) && !path.startsWith('/admin/auth')) {
      logout();
      throw new Error('Your session expired. Please sign in again.');
    }

    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }

    if (!res.ok) {
      let message = (data && (data.message || data.detail || data.error)) || ('Request failed (' + res.status + ')');
      if (data && Array.isArray(data.errors) && data.errors.length) {
        message = data.errors.map(e => (e.field ? e.field + ': ' : '') + (e.defaultMessage || '')).join(', ');
      }
      throw new Error(message);
    }
    return data;
  }

  async function getDomains() {
    if (!domainsCache) domainsCache = await api('/content/domains');
    return domainsCache;
  }

  // ---------- login / logout ----------
  function showLogin() {
    $('#appView').hidden = true;
    $('#loginView').hidden = false;
    $('#loginEmail').focus();
  }

  function showApp() {
    $('#loginView').hidden = true;
    $('#appView').hidden = false;
    $('#adminName').textContent = load(NAME_KEY) || '';
    route();
  }

  function logout() {
    token = null;
    domainsCache = null;
    store(TOKEN_KEY, null);
    store(NAME_KEY, null);
    showLogin();
  }

  $('#loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const err = $('#loginError');
    err.hidden = true;
    const btn = e.submitter || $('#loginForm button');
    btn.disabled = true;
    try {
      const data = await api('/admin/auth/login', {
        method: 'POST',
        body: { email: $('#loginEmail').value, password: $('#loginPassword').value }
      });
      token = data.token;
      store(TOKEN_KEY, token);
      store(NAME_KEY, data.fullName);
      $('#loginPassword').value = '';
      if (!location.hash) location.hash = '#dashboard';
      showApp();
    } catch (ex) {
      err.textContent = ex.message === 'Request failed (401)' ? 'Wrong email or password' : ex.message;
      err.hidden = false;
    } finally {
      btn.disabled = false;
    }
  });

  $('#logoutBtn').addEventListener('click', logout);

  // ---------- routing ----------
  function route() {
    if (!token) { showLogin(); return; }
    const key = (location.hash || '#dashboard').slice(1);
    const view = VIEWS[key] || VIEWS.dashboard;
    document.querySelectorAll('#nav a').forEach(a => a.classList.toggle('active', a.dataset.view === key));
    $('#pageTitle').textContent = view.title;
    $('#content').innerHTML = '<div class="empty">Loading...</div>';
    view.render().catch(ex => {
      $('#content').innerHTML = `<div class="panel"><div class="empty">Could not load: ${esc(ex.message)}</div></div>`;
    });
  }
  window.addEventListener('hashchange', route);

  // ---------- modal (shared Add/Edit pop-up) ----------
  let onSave = null;
  const modal = $('#modal');

  function openModal(title, bodyHtml, saveHandler, afterOpen) {
    $('#modalTitle').textContent = title;
    $('#modalBody').innerHTML = bodyHtml;
    $('#modalError').hidden = true;
    onSave = saveHandler;
    modal.showModal();
    if (afterOpen) afterOpen($('#modalBody'));
    const first = $('#modalBody').querySelector('input, select, textarea');
    if (first) first.focus();
  }

  modal.addEventListener('click', (e) => {
    if (e.target.closest('[data-close]')) modal.close();
  });

  $('#modalForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!onSave) return;
    const btn = $('#modalSave');
    btn.disabled = true;
    $('#modalError').hidden = true;
    try {
      await onSave($('#modalBody'));
      modal.close();
    } catch (ex) {
      $('#modalError').textContent = ex.message;
      $('#modalError').hidden = false;
    } finally {
      btn.disabled = false;
    }
  });

  const val = (root, name) => {
    const el = root.querySelector(`[name="${name}"]`);
    return el ? el.value.trim() : '';
  };
  const intOrNull = (s) => (s === '' ? null : parseInt(s, 10));

  // ================= DASHBOARD =================
  async function renderDashboard() {
    const s = await api('/admin/stats');
    const missed = s.mostMissed.length
      ? `<div class="table-wrap"><table>
           <thead><tr><th>#</th><th>Question</th><th class="num">Wrong</th><th class="num">Answered</th></tr></thead>
           <tbody>${s.mostMissed.map(m => `
             <tr><td>${m.questionId}</td><td><div class="clamp">${esc(m.questionText)}</div></td>
             <td class="num">${m.timesWrong}</td><td class="num">${m.timesAnswered}</td></tr>`).join('')}
           </tbody></table></div>`
      : '<div class="empty">No wrong answers recorded yet.</div>';

    $('#content').innerHTML = `
      <div class="stats">
        <div class="stat"><div class="stat-label">Users</div><div class="stat-value">${s.totalUsers}</div>
          <div class="stat-note">${s.activeUsers} active</div></div>
        <div class="stat"><div class="stat-label">Questions</div><div class="stat-value">${s.activeQuestions}</div>
          <div class="stat-note">${s.totalQuestions} total, incl. hidden</div></div>
        <div class="stat"><div class="stat-label">Road Signs</div><div class="stat-value">${s.activeRoadSigns}</div>
          <div class="stat-note">active</div></div>
        <div class="stat"><div class="stat-label">Violations</div><div class="stat-value">${s.activeViolations}</div>
          <div class="stat-note">active</div></div>
        <div class="stat"><div class="stat-label">Exams Taken</div><div class="stat-value">${s.examsTaken}</div>
          <div class="stat-note">${s.examsPassed} passed</div></div>
        <div class="stat"><div class="stat-label">Pass Rate</div><div class="stat-value">${s.passRate}%</div>
          <div class="stat-note">passing score 80%</div></div>
      </div>
      <div class="panel">
        <div class="panel-head"><h3>Most-missed questions</h3></div>
        ${missed}
      </div>`;
  }

  // ================= QUESTIONS =================
  let questionFilter = { search: '', license: '', vehicle: '', showHidden: false };

  async function renderQuestions() {
    const [rows, domains] = await Promise.all([api('/admin/questions'), getDomains()]);
    const domainName = Object.fromEntries(domains.map(d => [d.domainId, d.domainName]));

    $('#content').innerHTML = `
      <div class="panel">
        <div class="panel-head">
          <div class="toolbar">
            <input type="search" id="qSearch" placeholder="Search question text" value="${esc(questionFilter.search)}">
            <select id="qLicense"><option value="">All licenses</option>${options(LICENSES, questionFilter.license)}</select>
            <select id="qVehicle"><option value="">All vehicle types</option>${options(VEHICLES, questionFilter.vehicle)}</select>
            <label class="check"><input type="checkbox" id="qHidden" ${questionFilter.showHidden ? 'checked' : ''} style="width:auto;margin:0"> Show hidden</label>
          </div>
          <button class="btn btn-gold" id="qAdd">+ Add question</button>
        </div>
        <div id="qTable"></div>
      </div>`;

    function draw() {
      const f = questionFilter;
      const list = rows.filter(q =>
        (f.showHidden || q.isActive) &&
        (!f.license || q.licenseCategory === f.license) &&
        (!f.vehicle || q.vehicleType === f.vehicle) &&
        (!f.search || q.questionText.toLowerCase().includes(f.search.toLowerCase())));

      $('#qTable').innerHTML = list.length ? `
        <div class="table-wrap"><table>
          <thead><tr><th>#</th><th>Question</th><th>Answer</th><th>License</th><th>Vehicle</th><th>Domain</th><th>Status</th><th></th></tr></thead>
          <tbody>${list.map(q => `
            <tr class="${q.isActive ? '' : 'inactive'}">
              <td>${q.questionId}</td>
              <td><div class="clamp">${esc(q.questionText)}</div></td>
              <td><span class="badge badge-navy">${LETTERS[q.correctChoice - 1]}</span></td>
              <td>${esc(LICENSES[q.licenseCategory] || q.licenseCategory)}</td>
              <td>${esc(VEHICLES[q.vehicleType] || q.vehicleType)}</td>
              <td>${esc(domainName[q.domainId] || q.domainId)}</td>
              <td>${q.isActive ? '<span class="badge badge-green">Active</span>' : '<span class="badge badge-red">Hidden</span>'}</td>
              <td class="actions">
                <button class="btn btn-ghost btn-sm" data-edit="${q.questionId}">Edit</button>
                ${q.isActive ? `<button class="btn btn-danger btn-sm" data-hide="${q.questionId}">Hide</button>` : ''}
              </td>
            </tr>`).join('')}
          </tbody></table></div>
        <div class="empty" style="padding:10px">${list.length} of ${rows.length} questions shown</div>`
        : '<div class="empty">No questions match. Click "+ Add question" to add one.</div>';
    }
    draw();

    $('#qSearch').addEventListener('input', e => { questionFilter.search = e.target.value; draw(); });
    $('#qLicense').addEventListener('change', e => { questionFilter.license = e.target.value; draw(); });
    $('#qVehicle').addEventListener('change', e => { questionFilter.vehicle = e.target.value; draw(); });
    $('#qHidden').addEventListener('change', e => { questionFilter.showHidden = e.target.checked; draw(); });
    $('#qAdd').addEventListener('click', () => questionForm(null, domains));
    $('#qTable').addEventListener('click', async e => {
      const edit = e.target.closest('[data-edit]');
      const hide = e.target.closest('[data-hide]');
      if (edit) questionForm(rows.find(q => q.questionId === +edit.dataset.edit), domains);
      if (hide && confirm('Hide this question? It will no longer appear in the app. You can show it again with Edit.')) {
        try { await api('/admin/questions/' + hide.dataset.hide, { method: 'DELETE' }); toast('Question hidden'); route(); }
        catch (ex) { toast(ex.message, true); }
      }
    });
  }

  function questionForm(q, domains) {
    const isNew = !q;
    q = q || { domainId: domains[0] && domains[0].domainId, licenseCategory: 'NON_PROF', vehicleType: 'LIGHT', correctChoice: 1, isActive: true };
    const domainOptions = Object.fromEntries(domains.map(d => [String(d.domainId), d.domainName]));
    const choiceRows = LETTERS.map((L, i) => `
      <div class="choice-row">
        <input type="radio" name="correct" value="${i + 1}" ${q.correctChoice === i + 1 ? 'checked' : ''} title="Mark ${L} as the correct answer">
        <span class="choice-letter">${L}</span>
        <input type="text" name="choice${L}" required value="${esc(q['choice' + L])}" placeholder="Choice ${L}">
      </div>`).join('');

    openModal(isNew ? 'Add question' : 'Edit question #' + q.questionId, `
      <div class="grid-3">
        <label>License<select name="licenseCategory">${options(LICENSES, q.licenseCategory)}</select></label>
        <label>Vehicle type<select name="vehicleType">${options(VEHICLES, q.vehicleType)}</select></label>
        <label>Domain<select name="domainId">${options(domainOptions, String(q.domainId))}</select></label>
      </div>
      <label>Question<textarea name="questionText" required>${esc(q.questionText)}</textarea></label>
      <label>Choices <span class="hint">(select the circle beside the correct answer)</span></label>
      ${choiceRows}
      <label style="margin-top:12px">Explanation <span class="hint">(shown after answering; optional)</span>
        <textarea name="explanationText">${esc(q.explanationText)}</textarea></label>
      <div class="grid-2">
        <label>Source reference <span class="hint">e.g. LTO Driver's Manual Vol. 1, p. 160</span>
          <input name="sourceReference" value="${esc(q.sourceReference)}"></label>
        <label>Image file name <span class="hint">(road sign questions; optional)</span>
          <input name="graphicPath" value="${esc(q.graphicPath)}"></label>
      </div>
      <label class="check"><input type="checkbox" name="isActive" ${q.isActive ? 'checked' : ''} style="width:auto;margin:0"> Active (shown in the app)</label>
    `, async (root) => {
      const correct = root.querySelector('input[name="correct"]:checked');
      if (!correct) throw new Error('Select the correct answer');
      const body = {
        licenseCategory: val(root, 'licenseCategory'),
        vehicleType: val(root, 'vehicleType'),
        domainId: parseInt(val(root, 'domainId'), 10),
        questionText: val(root, 'questionText'),
        choiceA: val(root, 'choiceA'), choiceB: val(root, 'choiceB'),
        choiceC: val(root, 'choiceC'), choiceD: val(root, 'choiceD'),
        correctChoice: parseInt(correct.value, 10),
        explanationText: val(root, 'explanationText'),
        sourceReference: val(root, 'sourceReference'),
        graphicPath: val(root, 'graphicPath'),
        isActive: root.querySelector('[name="isActive"]').checked
      };
      await api(isNew ? '/admin/questions' : '/admin/questions/' + q.questionId,
        { method: isNew ? 'POST' : 'PUT', body: body });
      toast(isNew ? 'Question added' : 'Question saved');
      route();
    });
  }

  // ================= ROAD SIGNS =================
  async function renderRoadSigns() {
    const rows = await api('/admin/road-signs');
    $('#content').innerHTML = `
      <div class="panel">
        <div class="panel-head">
          <h3>${rows.filter(r => r.isActive).length} active signs</h3>
          <button class="btn btn-gold" id="sAdd">+ Add road sign</button>
        </div>
        ${rows.length ? `<div class="table-wrap"><table>
          <thead><tr><th>Order</th><th>Sign</th><th>Category</th><th>Meaning</th><th>Image</th><th>Status</th><th></th></tr></thead>
          <tbody>${rows.map(s => `
            <tr class="${s.isActive ? '' : 'inactive'}">
              <td class="num">${s.sortOrder}</td>
              <td><strong>${esc(s.signName)}</strong></td>
              <td><span class="badge badge-gold">${esc(SIGN_CATEGORIES[s.signCategory] || s.signCategory)}</span></td>
              <td><div class="clamp">${esc(s.description)}</div></td>
              <td>${s.imagePath ? esc(s.imagePath) : '<span class="hint">none yet</span>'}</td>
              <td>${s.isActive ? '<span class="badge badge-green">Active</span>' : '<span class="badge badge-red">Hidden</span>'}</td>
              <td class="actions">
                <button class="btn btn-ghost btn-sm" data-edit="${s.signId}">Edit</button>
                ${s.isActive ? `<button class="btn btn-danger btn-sm" data-hide="${s.signId}">Hide</button>` : ''}
              </td>
            </tr>`).join('')}
          </tbody></table></div>` : '<div class="empty">No road signs yet.</div>'}
      </div>`;

    $('#sAdd').addEventListener('click', () => roadSignForm(null, rows));
    $('#content .panel').addEventListener('click', async e => {
      const edit = e.target.closest('[data-edit]');
      const hide = e.target.closest('[data-hide]');
      if (edit) roadSignForm(rows.find(s => s.signId === +edit.dataset.edit), rows);
      if (hide && confirm('Hide this road sign from the app?')) {
        try { await api('/admin/road-signs/' + hide.dataset.hide, { method: 'DELETE' }); toast('Road sign hidden'); route(); }
        catch (ex) { toast(ex.message, true); }
      }
    });
  }

  function roadSignForm(s, rows) {
    const isNew = !s;
    const nextOrder = rows.reduce((m, r) => Math.max(m, r.sortOrder || 0), 0) + 1;
    s = s || { signCategory: 'REGULATORY', sortOrder: nextOrder, isActive: true };
    openModal(isNew ? 'Add road sign' : 'Edit road sign', `
      <div class="grid-2">
        <label>Sign name<input name="signName" required value="${esc(s.signName)}"></label>
        <label>Category<select name="signCategory">${options(SIGN_CATEGORIES, s.signCategory)}</select></label>
      </div>
      <label>Meaning / what the driver must do<textarea name="description" required>${esc(s.description)}</textarea></label>
      <div class="grid-2">
        <label>Image file name <span class="hint">e.g. sign_stop.png (optional)</span><input name="imagePath" value="${esc(s.imagePath)}"></label>
        <label>Display order<input name="sortOrder" type="number" min="0" value="${esc(s.sortOrder)}"></label>
      </div>
      <label class="check"><input type="checkbox" name="isActive" ${s.isActive ? 'checked' : ''} style="width:auto;margin:0"> Active (shown in the app)</label>
    `, async (root) => {
      const body = {
        signName: val(root, 'signName'),
        signCategory: val(root, 'signCategory'),
        description: val(root, 'description'),
        imagePath: val(root, 'imagePath'),
        sortOrder: intOrNull(val(root, 'sortOrder')),
        isActive: root.querySelector('[name="isActive"]').checked
      };
      await api(isNew ? '/admin/road-signs' : '/admin/road-signs/' + s.signId,
        { method: isNew ? 'POST' : 'PUT', body: body });
      toast(isNew ? 'Road sign added' : 'Road sign saved');
      route();
    });
  }

  // ================= VIOLATIONS =================
  async function renderViolations() {
    const rows = await api('/admin/violations');
    $('#content').innerHTML = `
      <div class="panel">
        <div class="panel-head">
          <h3>${rows.filter(r => r.isActive).length} active violations</h3>
          <button class="btn btn-gold" id="vAdd">+ Add violation</button>
        </div>
        ${rows.length ? `<div class="table-wrap"><table>
          <thead><tr><th>Order</th><th>Violation</th><th>Fines</th><th>Legal basis</th><th>Status</th><th></th></tr></thead>
          <tbody>${rows.map(v => `
            <tr class="${v.isActive ? '' : 'inactive'}">
              <td class="num">${v.sortOrder}</td>
              <td><strong>${esc(v.violationName)}</strong><div class="clamp hint">${esc(v.description)}</div></td>
              <td>${v.fines.map(f => `<div><span class="hint">${ordinal(f.offenseNumber)}:</span> <strong>${peso(f.fineAmount)}</strong>${f.additionalPenalty ? ` <span class="hint">+ ${esc(f.additionalPenalty)}</span>` : ''}</div>`).join('')}</td>
              <td>${esc(v.legalBasis || '')}</td>
              <td>${v.isActive ? '<span class="badge badge-green">Active</span>' : '<span class="badge badge-red">Hidden</span>'}</td>
              <td class="actions">
                <button class="btn btn-ghost btn-sm" data-edit="${v.violationId}">Edit</button>
                ${v.isActive ? `<button class="btn btn-danger btn-sm" data-hide="${v.violationId}">Hide</button>` : ''}
              </td>
            </tr>`).join('')}
          </tbody></table></div>` : '<div class="empty">No violations yet.</div>'}
      </div>`;

    $('#vAdd').addEventListener('click', () => violationForm(null, rows));
    $('#content .panel').addEventListener('click', async e => {
      const edit = e.target.closest('[data-edit]');
      const hide = e.target.closest('[data-hide]');
      if (edit) violationForm(rows.find(v => v.violationId === +edit.dataset.edit), rows);
      if (hide && confirm('Hide this violation from the app?')) {
        try { await api('/admin/violations/' + hide.dataset.hide, { method: 'DELETE' }); toast('Violation hidden'); route(); }
        catch (ex) { toast(ex.message, true); }
      }
    });
  }

  function fineRow(f) {
    const offense = { 1: '1st', 2: '2nd', 3: '3rd', 4: '4th & up' };
    return `<div class="fine-row">
      <label>Offense<select name="offenseNumber">${options(offense, String(f.offenseNumber))}</select></label>
      <label>Fine (₱)<input name="fineAmount" type="number" min="0" step="0.01" required value="${esc(f.fineAmount)}"></label>
      <label>Additional penalty <span class="hint">(optional)</span><input name="additionalPenalty" value="${esc(f.additionalPenalty)}"></label>
      <button type="button" class="btn btn-danger btn-sm" data-remove-fine title="Remove this tier">Remove</button>
    </div>`;
  }

  function violationForm(v, rows) {
    const isNew = !v;
    const nextOrder = rows.reduce((m, r) => Math.max(m, r.sortOrder || 0), 0) + 1;
    v = v || { sortOrder: nextOrder, isActive: true, fines: [{ offenseNumber: 1, fineAmount: '' }] };
    openModal(isNew ? 'Add violation' : 'Edit violation', `
      <label>Violation name<input name="violationName" required value="${esc(v.violationName)}"></label>
      <label>Description<textarea name="description" required>${esc(v.description)}</textarea></label>
      <div class="grid-3">
        <label>Legal basis <span class="hint">e.g. R.A. 8750</span><input name="legalBasis" value="${esc(v.legalBasis)}"></label>
        <label>Icon file name <span class="hint">(optional)</span><input name="imagePath" value="${esc(v.imagePath)}"></label>
        <label>Display order<input name="sortOrder" type="number" min="0" value="${esc(v.sortOrder)}"></label>
      </div>
      <label>Fine tiers</label>
      <div id="fines">${v.fines.map(fineRow).join('')}</div>
      <button type="button" class="btn btn-ghost btn-sm" id="addFine">+ Add tier</button>
      <label class="check" style="margin-top:14px"><input type="checkbox" name="isActive" ${v.isActive ? 'checked' : ''} style="width:auto;margin:0"> Active (shown in the app)</label>
    `, async (root) => {
      const fines = [...root.querySelectorAll('.fine-row')].map(r => ({
        offenseNumber: parseInt(r.querySelector('[name="offenseNumber"]').value, 10),
        fineAmount: parseFloat(r.querySelector('[name="fineAmount"]').value),
        additionalPenalty: r.querySelector('[name="additionalPenalty"]').value.trim()
      }));
      const body = {
        violationName: val(root, 'violationName'),
        description: val(root, 'description'),
        legalBasis: val(root, 'legalBasis'),
        imagePath: val(root, 'imagePath'),
        sortOrder: intOrNull(val(root, 'sortOrder')),
        isActive: root.querySelector('[name="isActive"]').checked,
        fines: fines
      };
      await api(isNew ? '/admin/violations' : '/admin/violations/' + v.violationId,
        { method: isNew ? 'POST' : 'PUT', body: body });
      toast(isNew ? 'Violation added' : 'Violation saved');
      route();
    }, (root) => {
      root.querySelector('#addFine').addEventListener('click', () => {
        const box = root.querySelector('#fines');
        const used = [...box.querySelectorAll('[name="offenseNumber"]')].map(s => +s.value);
        const next = [1, 2, 3, 4].find(n => !used.includes(n)) || 4;
        box.insertAdjacentHTML('beforeend', fineRow({ offenseNumber: next, fineAmount: '' }));
      });
      root.querySelector('#fines').addEventListener('click', e => {
        const btn = e.target.closest('[data-remove-fine]');
        if (btn && root.querySelectorAll('.fine-row').length > 1) btn.closest('.fine-row').remove();
      });
    });
  }

  // ================= USERS =================
  async function renderUsers() {
    const rows = await api('/admin/users');
    $('#content').innerHTML = `
      <div class="panel">
        <div class="panel-head"><h3>${rows.length} registered users</h3></div>
        ${rows.length ? `<div class="table-wrap"><table>
          <thead><tr><th>#</th><th>Name</th><th>Email</th><th>License</th><th class="num">Exams</th><th class="num">Streak</th><th>Joined</th><th>Status</th><th></th></tr></thead>
          <tbody>${rows.map(u => `
            <tr class="${u.isActive ? '' : 'inactive'}">
              <td>${u.userId}</td>
              <td><strong>${esc(u.fullName)}</strong></td>
              <td>${esc(u.email)}</td>
              <td>${esc(LICENSES[u.targetLicense] || u.targetLicense)}</td>
              <td class="num">${u.examsTaken}</td>
              <td class="num">${u.currentStreak} <span class="hint">(best ${u.longestStreak})</span></td>
              <td>${u.createdAt ? esc(u.createdAt.slice(0, 10)) : ''}</td>
              <td>${u.isActive ? '<span class="badge badge-green">Active</span>' : '<span class="badge badge-red">Deactivated</span>'}</td>
              <td class="actions">
                ${u.isActive
                  ? `<button class="btn btn-danger btn-sm" data-user="${u.userId}" data-active="false">Deactivate</button>`
                  : `<button class="btn btn-ghost btn-sm" data-user="${u.userId}" data-active="true">Reactivate</button>`}
              </td>
            </tr>`).join('')}
          </tbody></table></div>` : '<div class="empty">No users have registered yet.</div>'}
      </div>`;

    $('#content .panel').addEventListener('click', async e => {
      const btn = e.target.closest('[data-user]');
      if (!btn) return;
      const activate = btn.dataset.active === 'true';
      if (!activate && !confirm('Deactivate this account? The user will not be able to log in.')) return;
      try {
        await api('/admin/users/' + btn.dataset.user + '/active', { method: 'PUT', body: { isActive: activate } });
        toast(activate ? 'Account reactivated' : 'Account deactivated');
        route();
      } catch (ex) { toast(ex.message, true); }
    });
  }

  // ---------- start ----------
  if (token) showApp(); else showLogin();
})();
