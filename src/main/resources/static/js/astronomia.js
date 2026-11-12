(function () {
  'use strict';
  const byId = id => document.getElementById(id);
  const root = byId('orbit-scene');
  if (!root) return;
  const controls = { mass: byId('stellar-mass'), axis: byId('semi-major'), ecc: byId('eccentricity'), planet: byId('planet-mass'), speed: byId('time-speed') };
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const state = { angle: 0, playing: !reducedMotion, last: 0, model: null };
  const clamp = (n, min, max) => Math.min(max, Math.max(min, n));
  function calculate(mass, axis, eccentricity, planetMass) {
    if (![mass, axis, eccentricity, planetMass].every(Number.isFinite) || mass <= 0 || axis <= 0 || eccentricity < 0 || eccentricity >= 1 || planetMass <= 0) return null;
    const period = Math.sqrt(Math.pow(axis, 3) / (mass + planetMass * 3.003e-6));
    const peri = axis * (1 - eccentricity), aphe = axis * (1 + eccentricity);
    const circularVelocity = 29.7847 * Math.sqrt(mass / axis);
    const periVelocity = circularVelocity * Math.sqrt((1 + eccentricity) / (1 - eccentricity));
    const apheVelocity = circularVelocity * Math.sqrt((1 - eccentricity) / (1 + eccentricity));
    return { period, peri, aphe, circularVelocity, periVelocity, apheVelocity };
  }
  function model() { return calculate(Number(controls.mass.value), Number(controls.axis.value), Number(controls.ecc.value), Number(controls.planet.value)); }
  function renderMetrics(m) {
    byId('mass-output').textContent = Number(controls.mass.value).toFixed(2) + ' M☉';
    byId('axis-output').textContent = Number(controls.axis.value).toFixed(2) + ' UA';
    byId('ecc-output').textContent = Number(controls.ecc.value).toFixed(2);
    byId('planet-mass-output').textContent = Number(controls.planet.value).toFixed(1) + ' M⊕';
    byId('period-result').innerHTML = m.period.toFixed(2) + ' <small>años</small>';
    byId('velocity-result').innerHTML = m.circularVelocity.toFixed(2) + ' <small>km/s</small>';
    byId('peri-result').innerHTML = m.peri.toFixed(2) + ' <small>UA</small>';
    byId('aphe-result').innerHTML = m.aphe.toFixed(2) + ' <small>UA</small>';
    byId('insight-title').textContent = Number(controls.ecc.value) < .05 ? 'Una órbita casi circular' : Number(controls.ecc.value) < .4 ? 'Una órbita moderadamente elíptica' : 'Una órbita muy excéntrica';
    byId('insight-text').textContent = `El planeta alcanza aproximadamente ${m.periVelocity.toFixed(2)} km/s en el perihelio y ${m.apheVelocity.toFixed(2)} km/s en el afelio. La gravedad acelera el planeta al acercarse a la estrella y lo desacelera al alejarse.`;
    const a = Number(controls.axis.value), e = Number(controls.ecc.value);
    const scale = 245 / (a * (1 + e));
    const rx = clamp(a * scale, 55, 245), ry = clamp(rx * Math.sqrt(1 - e * e), 35, 210);
    const cx = 360, cy = 220, focus = cx - rx * e;
    ['orbit-path', 'orbit-halo'].forEach(id => { byId(id).setAttribute('cx', cx); byId(id).setAttribute('cy', cy); byId(id).setAttribute('rx', rx); byId(id).setAttribute('ry', ry); });
    ['star-aura', 'star-core'].forEach(id => byId(id).setAttribute('cx', focus));
    byId('star-label').setAttribute('x', focus);
    state.model = { a, e, rx, ry, focus, cx, cy, scale, ...m };
    paintPlanet();
  }
  function paintPlanet() {
    const m = state.model; if (!m) return;
    const E = state.angle;
    const x = m.cx + m.rx * Math.cos(E), y = m.cy + m.ry * Math.sin(E);
    byId('planet').setAttribute('cx', x); byId('planet').setAttribute('cy', y);
    byId('planet-aura').setAttribute('cx', x); byId('planet-aura').setAttribute('cy', y);
    byId('planet-label').setAttribute('x', x); byId('planet-label').setAttribute('y', y - 29);
    const vx = -m.rx * Math.sin(E), vy = m.ry * Math.cos(E), len = Math.hypot(vx, vy) || 1;
    const vector = byId('velocity-vector'); vector.setAttribute('transform', `translate(${x} ${y}) rotate(${Math.atan2(vy, vx) * 180 / Math.PI})`);
  }
  function frame(now) {
    if (!root.isConnected) return;
    if (state.last && state.playing && state.model) {
      const dt = Math.min((now - state.last) / 1000, .05), speed = Number(controls.speed.value);
      // Eccentric anomaly advances faster near periapsis, in accordance with Kepler's second law.
      const rate = 1.05 / (state.model.period * (1 + state.model.e * Math.cos(state.angle)));
      state.angle = (state.angle + dt * speed * rate) % (Math.PI * 2);
      paintPlanet();
    }
    state.last = now; window.requestAnimationFrame(frame);
  }
  function update() { const m = model(); if (m) renderMetrics(m); }
  Object.values(controls).forEach(control => control.addEventListener('input', update));
  byId('speed-value').textContent = Number(controls.speed.value).toFixed(1) + '×';
  controls.speed.addEventListener('input', () => { byId('speed-value').textContent = Number(controls.speed.value).toFixed(1) + '×'; });
  byId('play-orbit').addEventListener('click', event => { state.playing = !state.playing; event.currentTarget.textContent = state.playing ? 'Ⅱ Pausar' : '▶ Reanudar'; });
  byId('reset-orbit').addEventListener('click', () => { state.angle = 0; paintPlanet(); });
  document.querySelectorAll('[data-preset]').forEach(button => button.addEventListener('click', () => {
    const presets = { earth: [1, 1, .017, 1], mars: [1, 1.524, .093, .107], eccentric: [1, 1.6, .62, 1.4] };
    const values = presets[button.dataset.preset]; [controls.mass, controls.axis, controls.ecc, controls.planet].forEach((input, i) => input.value = values[i]); state.angle = 0; update();
  }));
  update(); window.requestAnimationFrame(frame);
  window.CHANGEAstronomy = { calculate };
})();
