/* CHANGE · Simulador de lanzamiento educativo — motor sólido idealizado.
   Modelo de un cohete educativo: F constante, propelente de masa variable,
   sin arrastre ni rotación. Sirve para entender TWR y Δv (Tsiolkovsky).
   No diseña cohetes reales ni representa vuelo. */
(function () {
  'use strict';
  const G = 9.80665;

  // Parámetros: masa del esqueleto (kg), propelente (kg), empuje medio (N), velocidad de escape (m/s).
  // Devuelve magnitudes idealizadas o null si la entrada no es válida.
  function solve(emptyMass, propMass, thrust, exhaustVelocity) {
    if (![emptyMass, propMass, thrust, exhaustVelocity].every(Number.isFinite)) return null;
    if (emptyMass <= 0 || propMass <= 0 || thrust <= 0 || exhaustVelocity <= 0) return null;
    const m0 = emptyMass + propMass;
    const massFlow = thrust / exhaustVelocity;         // Ƴ = F / vₑ  [kg/s]
    const burnTime = propMass / massFlow;              // t = m_p / Ƴ  [s]
    const acceleration = thrust / m0 - G;               // a = F/m − g  [m/s²]
    const velocityEndBurn = acceleration * burnTime;    // v = a · t  [m/s]
    const burnDistance = 0.5 * acceleration * burnTime * burnTime;   // h_quemad  [m]
    const coastDistance = velocityEndBurn > 0 ? (velocityEndBurn * velocityEndBurn) / (2 * G) : 0; // h libre [m]
    const deltaV = exhaustVelocity * Math.log(m0 / emptyMass);       // Tsiolkovsky [m/s]
    const twr = thrust / (m0 * G);
    const isp = exhaustVelocity / G;                    // s
    return {
      massFlow, burnTime, acceleration, velocityEndBurn,
      burnDistance, coastDistance,
      maxAltitude: burnDistance + coastDistance,
      deltaV, twr, isp,
      takesOff: acceleration > 0,
    };
  }

  if (typeof module !== 'undefined') module.exports = { solve };
  if (typeof document === 'undefined') return;

  const $ = id => document.getElementById(id);
  const massEmpty = $('rl-mass-empty'), massProp = $('rl-mass-prop'), thrust = $('rl-thrust'), ve = $('rl-ve');
  if (!massEmpty) return;

  const labels = [
    ['rl-mass-empty-value', () => massEmpty.value + ' g'],
    ['rl-mass-prop-value', () => massProp.value + ' g'],
    ['rl-thrust-value', () => thrust.value + ' N'],
    ['rl-ve-value', () => ve.value + ' m/s'],
  ];

  function fmt(n, unit, digits) {
    const value = Number.isFinite(n) ? n : 0;
    const abs = Math.abs(value);
    const str = abs >= 1000 ? value.toFixed(Math.max(0, (digits || 0))) : value.toFixed(digits == null ? 2 : digits);
    return str + ' ' + unit;
  }

  function render() {
    labels.forEach(([id, fn]) => { $(id).textContent = fn(); });
    const r = solve(massEmpty.value / 1000, massProp.value / 1000, Number(thrust.value), Number(ve.value));
    if (!r) { ['rl-isp','rl-burn','rl-acc','rl-alt','rl-deltav','rl-twr','rl-veredicto'].forEach(id => $(id).textContent = '—'); return; }
    $('rl-isp').textContent = fmt(r.isp, 's', 1);
    $('rl-burn').textContent = fmt(r.burnTime, 's', 3);
    $('rl-acc').textContent = fmt(r.acceleration, 'm/s² (' + (r.acceleration / G).toFixed(1) + ' g)', 1);
    $('rl-alt').textContent = fmt(r.maxAltitude, 'm', 2);
    $('rl-deltav').textContent = fmt(r.deltaV, 'm/s', 1);
    $('rl-twr').textContent = fmt(r.twr, 'x', 2);
    $('rl-veredicto').textContent = r.takesOff
      ? '>1 TWR: despega' : 'TWR ≤ 1: no despega';
    $('rl-veredicto').classList.toggle('badge-ok', r.takesOff);
  }

  [massEmpty, massProp, thrust, ve].forEach(el => el.addEventListener('input', render));
  render();
})();
