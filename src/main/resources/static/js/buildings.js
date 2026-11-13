document.addEventListener('DOMContentLoaded', () => {
  const search = document.getElementById('buildingSearch');
  if (search) {
    search.addEventListener('input', () => {
      const query = search.value.toLocaleLowerCase().trim();
      let visible = 0;
      document.querySelectorAll('.building-card[data-filter]').forEach(card => {
        const match = card.dataset.filter.toLocaleLowerCase().includes(query);
        card.hidden = !match;
        if (match) visible++;
      });
      const empty = document.getElementById('searchNoResults');
      if (empty) empty.hidden = visible !== 0;
    });
  }
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
  });
});
