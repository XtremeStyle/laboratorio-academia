/* CHANGE · SPACE-LAB OS — barra de estado inferior.
   Telemetría decorativa (marcable como simulada); no declara datos reales. */
(() => {
  const bar = document.getElementById('os-terminal');
  if (!bar) return;
  const log = bar.querySelector('.os-log');
  const base = [
    'MEM // sesión activa y canal seguro',
    'NAV // rutas confirmables · academia parametrizada',
    'SYS // XP recalculado al cerrar etapa',
    'LAB // 8 lecciones por ruta · misión conectada',
    'LOG // copia privada excluye datos y credenciales',
  ];
  const host = location.pathname.replace(/^\/+/, '') || 'inicio';
  const msgs = [
    'NAV // ruta: /' + host,
    ...base,
  ];
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) {
    log.innerHTML = '<b>NAV //</b> ruta: /' + host;
    return;
  }
  let i = 0;
  const tick = () => {
    i = (i + 1) % msgs.length;
    const [tag, ...rest] = msgs[i].split('//');
    log.innerHTML = '<b>' + tag.trim() + ' //</b>' + rest.join('//');
  };
  tick();
  setInterval(tick, 4200);
  // Eco de envíos de formulario en la barra (no bloquea ni registra datos).
  document.addEventListener('submit', (event) => {
    const form = event.target;
    if (form instanceof HTMLFormElement) {
      const action = (form.getAttribute('action') || '').split('?')[0];
      log.innerHTML = '<b>LOG //</b> transmitiendo «' + action.replace(/^\/+/, '') + '»…';
    }
  });
})();
