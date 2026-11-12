/* CHANGE · Fases de la Luna — iluminación por píxel con normal de esfera.
   Modelo geométrico correcto para disco visible (sin atmósfera ni libración). */
(function () {
  'use strict';
  const SINODIC = 29.53059;

  function illumination(day) {
    if (!Number.isFinite(day)) return null;
    const d = ((day % SINODIC) + SINODIC) % SINODIC;
    return (1 - Math.cos((2 * Math.PI * d) / SINODIC)) / 2;
  }

  function phaseName(day) {
    if (!Number.isFinite(day)) return '—';
    const d = ((day % SINODIC) + SINODIC) % SINODIC;
    if (d < 1.84566) return 'Luna nueva';
    if (d < 7.38295) return 'Creciente';
    if (d < 9.22830) return 'Cuarto creciente';
    if (d < 14.76524) return 'Gibosa creciente';
    if (d < 16.61059) return 'Luna llena';
    if (d < 22.14753) return 'Gibosa menguante';
    if (d < 23.99289) return 'Cuarto menguante';
    if (d < 29.53059 - 0.923) return 'Menguante';
    return 'Luna nueva';
  }

  function drawMoon(canvas, day) {
    const ctx = canvas.getContext('2d');
    const W = canvas.width, H = canvas.height, cx = W / 2, cy = H / 2, r = Math.min(W, H) / 2 - 8;
    ctx.clearRect(0, 0, W, H);
    ctx.beginPath(); ctx.arc(cx, cy, r, 0, 2 * Math.PI); ctx.fillStyle = '#121724'; ctx.fill();
    const phase = ((day % SINODIC) + SINODIC) % SINODIC / SINODIC;
    const sx = Math.sin(2 * Math.PI * phase);
    const sz = -Math.cos(2 * Math.PI * phase);
    const img = ctx.getImageData(0, 0, W, H);
    const data = img.data;
    const litColor = [232, 226, 207], darkColor = [18, 23, 36];
    for (let y = 0; y < H; y++) {
      for (let x = 0; x < W; x++) {
        const dx = (x - cx) / r, dy = (y - cy) / r;
        const d2 = dx * dx + dy * dy;
        if (d2 > 1) continue;
        const nz = Math.sqrt(Math.max(0, 1 - d2));
        const inc = dx * sx + nz * sz;
        const t = Math.max(0, Math.min(1, (inc + 0.08) / 0.16));     // borde suave del terminador
        const idx = (y * W + x) * 4;
        data[idx] = Math.round(darkColor[0] + (litColor[0] - darkColor[0]) * t);
        data[idx + 1] = Math.round(darkColor[1] + (litColor[1] - darkColor[1]) * t);
        data[idx + 2] = Math.round(darkColor[2] + (litColor[2] - darkColor[2]) * t);
        data[idx + 3] = 255;
      }
    }
    ctx.putImageData(img, 0, 0);
  }

  if (typeof module !== 'undefined') module.exports = { illumination, phaseName, SINODIC };
  if (typeof document === 'undefined') return;

  const $ = id => document.getElementById(id);
  const slider = $('moon-day');
  if (!slider) return;
  const canvas = $('moon-canvas');
  const phaseLabel = $('moon-phase');
  const detail = $('moon-detail');
  const output = $('moon-day-value');

  function render() {
    const day = Number(slider.value);
    output.textContent = day.toFixed(1).replace('.', ',') + ' días';
    const pct = Math.round(illumination(day) * 1000) / 10;
    phaseLabel.textContent = phaseName(day);
    detail.textContent = `Iluminación aproximada: ${String(pct).replace('.', ',')}% del disco visible`;
    drawMoon(canvas, day);
  }
  slider.addEventListener('input', render);
  document.querySelectorAll('[data-moon]').forEach(button => button.addEventListener('click', () => { slider.value = button.getAttribute('data-moon'); render(); }));
  render();
})();
