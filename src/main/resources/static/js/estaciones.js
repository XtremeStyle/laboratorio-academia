(function (root) {
  'use strict';
  const AU_KM = 149597870.7, LIGHT_KM_S = 299792.458;
  function signalSeconds(au) { return Number.isFinite(au) && au >= 0 ? au * AU_KM / LIGHT_KM_S : null; }
  function robotStopped(distance, threshold) { return distance <= threshold; }
  const model = { signalSeconds, robotStopped };
  if (typeof module !== 'undefined') module.exports = model;
  if (typeof document === 'undefined') return;
  const byId = id => document.getElementById(id);
  const distance = byId('robot-distance'), threshold = byId('robot-threshold');
  if (distance && threshold) {
    const updateRobot = () => {
      const d = Number(distance.value), t = Number(threshold.value), stopped = robotStopped(d, t);
      byId('distance-value').textContent = `${d} cm`; byId('threshold-value').textContent = `${t} cm`;
      byId('robot-state').textContent = stopped ? 'Detenido · obstáculo detectado' : 'Avanzando · camino despejado';
      byId('robot-explanation').textContent = stopped ? `${d} cm está dentro del umbral de ${t} cm.` : `${d} cm supera el umbral de ${t} cm.`;
      byId('robot-icon').classList.toggle('stopped', stopped);
    };
    [distance, threshold].forEach(input => input.addEventListener('input', updateRobot));
    byId('robot-example').addEventListener('click', () => { distance.value = '15'; threshold.value = '30'; updateRobot(); });
    byId('robot-reset').addEventListener('click', () => { distance.value = '60'; threshold.value = '30'; updateRobot(); });
    updateRobot();
  }
  const light = byId('light-distance');
  if (light) {
    const format = seconds => { const rounded = Math.round(seconds); const hours = Math.floor(rounded / 3600), minutes = Math.floor((rounded % 3600) / 60), secs = rounded % 60; return (hours ? `${hours} h ` : '') + `${minutes} min ${secs} s`; };
    const updateLight = () => { const au = Number(light.value), seconds = signalSeconds(au); byId('light-distance-value').textContent = `${au.toFixed(2)} UA`; byId('light-result').textContent = format(seconds); byId('light-roundtrip').textContent = `Ida y vuelta: ${format(seconds * 2)}`; };
    light.addEventListener('input', updateLight);
    document.querySelectorAll('[data-light]').forEach(button => button.addEventListener('click', () => { light.value = button.dataset.light; updateLight(); }));
    updateLight();
  }
})(typeof window !== 'undefined' ? window : globalThis);
