/* CHANGE · PWM — forma de onda rectangular didáctica e ideal; sin driver ni carga real. */
(function () {
  'use strict';

  // Voltaje promedio en la carga: Vmedia = duty/100 × VCC.
  function averageVoltage(dutyPercent, vcc) {
    if (!Number.isFinite(dutyPercent) || !Number.isFinite(vcc)) return null;
    if (dutyPercent < 0 || dutyPercent > 100) return null;
    return (dutyPercent / 100) * vcc;
  }

  function draw(canvas, duty, freq) {
    const ctx = canvas.getContext('2d');
    const W = canvas.width, H = canvas.height;
    ctx.clearRect(0, 0, W, H);
    ctx.fillStyle = '#05090f'; ctx.fillRect(0, 0, W, H);
    // rejilla
    ctx.strokeStyle = 'rgba(120,215,255,.10)'; ctx.lineWidth = 1;
    for (let gy = 0.2; gy <= 0.8; gy += 0.2) { ctx.beginPath(); ctx.moveTo(0, H * gy); ctx.lineTo(W, H * gy); ctx.stroke(); }
    const period = W / 4;             // 4 ciclos visibles
    const highH = H * (1 - duty / 100) + 18;
    const lowH = H - 14;
    ctx.beginPath();
    ctx.moveTo(0, highH);
    for (let i = 0; i < 4; i++) {
      const x = i * period;
      const edge = (duty / 100) * period;
      ctx.lineTo(x + edge, highH);   // tramo activo
      ctx.lineTo(x + edge, lowH);    // bajada
      ctx.lineTo(x + period, lowH);  // tramo en reposo
      if (x + period < W) ctx.lineTo(x + period, highH); // subida
    }
    ctx.lineTo(W, lowH);
    ctx.strokeStyle = '#00e5ff';
    ctx.lineWidth = 2.4;
    ctx.shadowBlur = 10;
    ctx.shadowColor = 'rgba(0,229,255,.6)';
    ctx.stroke();
    ctx.shadowBlur = 0;
    ctx.fillStyle = 'rgba(232,238,248,.75)';
    ctx.font = '11px monospace';
    ctx.fillText('5V', 4, highH);
    ctx.fillText('0V', 4, lowH);
    ctx.fillText(freq.toFixed(1) + ' Hz', W - 44, 16);
  }

  if (typeof module !== 'undefined') module.exports = { averageVoltage };
  if (typeof document === 'undefined') return;

  const $ = id => document.getElementById(id);
  const duty = $('pwm-duty'), freq = $('pwm-freq');
  if (!duty) return;
  const dutyOut = $('pwm-duty-value'), freqOut = $('pwm-freq-value'), vavgOut = $('pwm-vavg'), summaryOut = $('pwm-summary');
  const canvas = $('pwm-canvas');

  function render() {
    const d = Number(duty.value), f = Number(freq.value);
    dutyOut.textContent = Math.round(d) + '%';
    freqOut.textContent = String(f).replace('.', ',') + ' Hz';
    const vavg = averageVoltage(d, 5);
    vavgOut.textContent = vavg.toFixed(2).replace('.', ',') + ' V';
    summaryOut.textContent = `Relación marca-espacio: ${Math.round(d)}% activa / ${100 - Math.round(d)}% en reposo · ${f} Hz · ${(1000 / f).toFixed(1)} ms por ciclo`;
    draw(canvas, d, f);
  }
  [duty, freq].forEach(el => el.addEventListener('input', render));
  document.querySelectorAll('[data-duty]').forEach(button => button.addEventListener('click', () => { duty.value = button.getAttribute('data-duty'); render(); }));
  render();
})();
