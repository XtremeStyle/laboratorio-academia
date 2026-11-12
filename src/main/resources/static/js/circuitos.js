const LaboratorioCircuitos = (() => {
  function calcular(v, r, vf, maxMa, maxW, cerrado) {
    if (![v, r, vf, maxMa, maxW].every(Number.isFinite) || v < 0 || v > 24 || r < 1 || r > 1000000 || vf < 0 || vf > 10 || maxMa < 0.1 || maxMa > 1000 || maxW < 0.01 || maxW > 100) return null;
    const corriente = cerrado ? Math.max(0, v - vf) / r : 0;
    const potencia = corriente * corriente * r;
    return { corriente, potencia, excesoLed: corriente * 1000 > maxMa, excesoResistencia: potencia > maxW, cerrado };
  }
  return { calcular };
})();
if (typeof module !== 'undefined') module.exports = LaboratorioCircuitos;
if (typeof document !== 'undefined') {
  const form = document.querySelector('#circuit-form');
  if (form) {
    const get = id => document.getElementById(id);
    function actualizar() {
      const campos = ['voltage', 'resistance', 'forward', 'max-current', 'power-rating'];
      const values = campos.map(id => get(id).value.trim() === '' ? NaN : Number(get(id).value));
      const state = LaboratorioCircuitos.calcular(...values, get('closed').checked);
      const lamp = get('led-lamp'); lamp.classList.remove('on', 'warning');
      get('switch-arm').setAttribute('y2', get('closed').checked ? '50' : '35');
      if (!state) { get('current').textContent = '—'; get('power').textContent = '—'; get('result').textContent = 'Revisa los valores indicados.'; return; }
      get('current').textContent = (state.corriente * 1000).toFixed(2) + ' mA';
      get('power').textContent = state.potencia.toFixed(3) + ' W';
      if (!state.cerrado) get('result').textContent = 'Circuito abierto: no circula corriente.';
      else if (state.corriente === 0) get('result').textContent = 'La batería no supera la caída del LED.';
      else if (state.excesoLed || state.excesoResistencia) { get('result').textContent = 'Se supera el límite de ' + (state.excesoLed && state.excesoResistencia ? 'LED y resistencia.' : state.excesoLed ? 'corriente del LED.' : 'potencia de la resistencia.'); lamp.classList.add('warning'); }
      else { get('result').textContent = 'Circuito cerrado dentro de los límites indicados.'; lamp.classList.add('on'); }
    }
    form.addEventListener('input', actualizar); form.addEventListener('submit', event => event.preventDefault()); actualizar();
  }
}
