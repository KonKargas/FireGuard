(() => {
  const root = document.documentElement;
  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('sidebarOverlay');
  const menuToggle = document.getElementById('menuToggle');
  const themeToggle = document.getElementById('themeToggle');
  const closeMenu = () => {
    document.body.classList.remove('sidebar-open');
    if (menuToggle) menuToggle.setAttribute('aria-expanded', 'false');
  };
  menuToggle?.addEventListener('click', () => {
    const opened = document.body.classList.toggle('sidebar-open');
    menuToggle.setAttribute('aria-expanded', String(opened));
  });
  overlay?.addEventListener('click', closeMenu);
  sidebar?.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));
  document.addEventListener('keydown', e => { if (e.key === 'Escape') closeMenu(); });
  themeToggle?.addEventListener('click', () => {
    const next = root.dataset.theme === 'light' ? 'dark' : 'light';
    root.dataset.theme = next;
    try { localStorage.setItem('fireguard-theme', next); } catch (e) { /* Storage disabled */ }
  });
  // Shared, declarative filtering for building/floor/sensor cards.
  const searchConfigs = [
    ['buildingSearch', '.building-card[data-filter]', 'filter', 'searchNoResults'],
    ['floorSearch', '.floor-card[data-filter]', 'filter', 'floorSearchEmpty'],
    ['sensorSearch', '.sensor-card[data-search]', 'search', 'sensorSearchEmpty']
  ];
  for (const [inputId, selector, field, emptyId] of searchConfigs) {
    const input = document.getElementById(inputId);
    if (!input) continue;
    const cards = [...document.querySelectorAll(selector)];
    const empty = document.getElementById(emptyId);
    const filter = () => {
      const query = input.value.trim().toLocaleLowerCase();
      let count = 0;
      for (const card of cards) {
        const visible = (card.dataset[field] || '').toLocaleLowerCase().includes(query);
        card.hidden = !visible;
        if (visible) count++;
      }
      if (empty) empty.hidden = count > 0 || cards.length === 0;
    };
    input.addEventListener('input', filter);
  }
  // Delete confirmations belong to the layout script rather than a particular module.
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
  });
  const year = document.getElementById('year');
  if (year) year.textContent = new Date().getFullYear();
})();
