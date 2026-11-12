(() => {
  const ready = () => {
    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
      const root = document.documentElement;
      const themeIcon = document.getElementById('themeIcon');
      const savedTheme = localStorage.getItem('laboratorio-theme');
      const setTheme = (theme) => {
        root.dataset.theme = theme;
        if (themeIcon) themeIcon.textContent = theme === 'light' ? '☾' : '☼';
        themeToggle.setAttribute('aria-label', theme === 'light' ? 'Activar tema oscuro' : 'Activar tema claro');
        localStorage.setItem('laboratorio-theme', theme);
      };
      setTheme(savedTheme === 'light' ? 'light' : 'dark');
      themeToggle.addEventListener('click', () => setTheme(root.dataset.theme === 'dark' ? 'light' : 'dark'));
    }
    const todayLabel = document.getElementById('todayLabel');
    if (todayLabel) {
      const date = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
      todayLabel.textContent = date.toLocaleUpperCase('es-CO');
    }
    const sidebar = document.getElementById('sidebar');
    const scrim = document.getElementById('mobileScrim');
    const mobileMenu = document.getElementById('mobileMenu');
    const closeMenu = () => {
      sidebar?.classList.remove('is-open');
      scrim?.classList.remove('is-visible');
    };
    mobileMenu?.addEventListener('click', () => {
      sidebar?.classList.toggle('is-open');
      scrim?.classList.toggle('is-visible');
    });
    scrim?.addEventListener('click', closeMenu);
    document.querySelectorAll('.nav-link').forEach(link => link.addEventListener('click', () => {
      document.querySelectorAll('.nav-link').forEach(item => item.classList.remove('active'));
      link.classList.add('active');
      closeMenu();
    }));
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', ready);
  else ready();
})();
