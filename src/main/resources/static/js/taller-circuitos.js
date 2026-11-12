(function () {
  'use strict';
  // Nodos 0 y 1 son los bornes ideales de la fuente; 2 y 3 son puntos de conexión.
  function solve(voltage, components) {
    if (!Number.isFinite(voltage) || voltage < 0 || voltage > 24 || !Array.isArray(components) || components.length > 8) return null;
    if (components.some(c => !Number.isInteger(c.a) || !Number.isInteger(c.b) || c.a < 0 || c.a > 3 || c.b < 0 || c.b > 3 || c.a === c.b || !Number.isFinite(c.r) || c.r < 1 || c.r > 1000000)) return null;
    const reachable = new Set([0, 1]);
    let changed = true;
    while (changed) { changed = false; for (const c of components) if (reachable.has(c.a) !== reachable.has(c.b)) { reachable.add(c.a); reachable.add(c.b); changed = true; } }
    const fromPositive = new Set([1]);
    changed = true;
    while (changed) { changed = false; for (const c of components) if (fromPositive.has(c.a) !== fromPositive.has(c.b)) { fromPositive.add(c.a); fromPositive.add(c.b); changed = true; } }
    const unknown = [2, 3].filter(n => reachable.has(n)), values = [0, voltage, null, null];
    const matrix = unknown.map(n => { const row = unknown.map(() => 0); let rhs = 0; for (const c of components) { if (c.a !== n && c.b !== n) continue; const other = c.a === n ? c.b : c.a, g = 1 / c.r; row[unknown.indexOf(n)] += g; if (unknown.includes(other)) row[unknown.indexOf(other)] -= g; else rhs += g * values[other]; } return [...row, rhs]; });
    for (let i = 0; i < unknown.length; i++) { const pivot = matrix[i][i]; if (Math.abs(pivot) < 1e-15) return null; for (let j = i; j <= unknown.length; j++) matrix[i][j] /= pivot; for (let k = 0; k < unknown.length; k++) if (k !== i) { const factor = matrix[k][i]; for (let j = i; j <= unknown.length; j++) matrix[k][j] -= factor * matrix[i][j]; } }
    unknown.forEach((n, i) => values[n] = matrix[i][unknown.length]);
    const parts = components.map(c => { const floating = values[c.a] === null || values[c.b] === null; const delta = floating ? 0 : values[c.a] - values[c.b]; return { ...c, voltage: Math.abs(delta), current: Math.abs(delta / c.r), power: delta * delta / c.r, floating }; });
    const sourceCurrent = parts.reduce((sum, c) => c.a === 1 ? sum + (values[c.a] - values[c.b]) / c.r : c.b === 1 ? sum + (values[c.b] - values[c.a]) / c.r : sum, 0);
    return { values, parts, sourceCurrent: Math.max(0, sourceCurrent), connected: fromPositive.has(0) };
  }
  if (typeof module !== 'undefined') module.exports = { solve };
  if (typeof document === 'undefined') return;
  const board = document.getElementById('red-circuitos'); if (!board) return;
  const get = id => document.getElementById(id); let parts = [{a:1,b:2,r:220},{a:2,b:0,r:330}];
  const labels = ['−', '+', 'A', 'B'], coords = [[45,180],[45,40],[310,40],[310,180]];
  const options = selected => labels.map((label,i) => `<option value="${i}"${selected===i?' selected':''}>${label}</option>`).join('');
  function render() {
    const rawVoltage=get('red-voltage').value.trim();
    const voltage = rawVoltage === '' ? NaN : Number(rawVoltage), result = solve(voltage, parts), table = get('red-components');
    table.replaceChildren();
    parts.forEach((part,i) => {
      const row = document.createElement('div'); row.className='network-row';
      row.innerHTML = `<strong>R${i+1}</strong><label>Desde<select data-index="${i}" data-field="a">${options(part.a)}</select></label><label>Hasta<select data-index="${i}" data-field="b">${options(part.b)}</select></label><label>Resistencia Ω<input type="number" min="1" max="1000000" step="1" value="${part.r}" data-index="${i}" data-field="r"></label><button type="button" data-remove="${i}" aria-label="Quitar resistencia ${i+1}">✕</button>`;
      table.appendChild(row);
    });
    const svg = get('red-diagram'), NS='http://www.w3.org/2000/svg'; svg.replaceChildren();
    parts.forEach((p,i) => { const [x1,y1]=coords[p.a], [x2,y2]=coords[p.b], path=document.createElementNS(NS,'path'); path.setAttribute('d',`M ${x1} ${y1} Q ${(x1+x2)/2+(i%3-1)*16} ${(y1+y2)/2+(i%3-1)*16} ${x2} ${y2}`); path.setAttribute('class','network-wire'); svg.appendChild(path); const t=document.createElementNS(NS,'text'); t.setAttribute('x',(x1+x2)/2+(i%3-1)*16);t.setAttribute('y',(y1+y2)/2+(i%3-1)*16-5);t.textContent=`R${i+1}`;svg.appendChild(t); });
    coords.forEach(([x,y],i) => { const c=document.createElementNS(NS,'circle');c.setAttribute('cx',x);c.setAttribute('cy',y);c.setAttribute('r',12);c.setAttribute('class','network-node');svg.appendChild(c);const t=document.createElementNS(NS,'text');t.setAttribute('x',x);t.setAttribute('y',y+4);t.setAttribute('text-anchor','middle');t.setAttribute('class','network-node-label');t.textContent=labels[i];svg.appendChild(t); });
    const output=get('red-result');
    if (!result) { output.textContent='Revisa los valores: cada resistencia debe conectar dos nodos distintos y medir entre 1 y 1.000.000 Ω.'; get('red-readings').replaceChildren(); const mmV=get('mm-voltage'),mmI=get('mm-current'),mmP=get('mm-power'); if (mmV) { mmV.textContent='—'; mmI.textContent='—'; mmP.textContent='—'; } return; }
    output.textContent = !result.connected ? 'Circuito abierto: no hay un camino completo entre + y −.' : `Corriente de la fuente: ${(result.sourceCurrent*1000).toFixed(2)} mA`;
    const readings=get('red-readings');readings.replaceChildren();result.parts.forEach((p,i)=> {const li=document.createElement('li');li.textContent=`R${i+1}: ${p.voltage.toFixed(2)} V · ${(p.current*1000).toFixed(2)} mA · ${p.power.toFixed(3)} W${p.floating?' · conexión aislada':''}`;readings.appendChild(li);});
    const mmV=get('mm-voltage'),mmI=get('mm-current'),mmP=get('mm-power');
    if (mmV) { const volts=Number(get('red-voltage').value)||0; mmV.textContent = volts.toFixed(2)+' V'; mmI.textContent = (result.sourceCurrent*1000).toFixed(2)+' mA'; mmP.textContent = (volts*result.sourceCurrent).toFixed(3)+' W'; }
  }
  get('red-components').addEventListener('change', event => { const el=event.target; if (!el.dataset.field) return; parts[Number(el.dataset.index)][el.dataset.field]=Number(el.value);render(); });
  get('red-components').addEventListener('click', event => { const el=event.target.closest('[data-remove]');if(el){parts.splice(Number(el.dataset.remove),1);render();} });
  get('red-voltage').addEventListener('input',render);
  get('red-add').addEventListener('click',()=>{if(parts.length<8){parts.push({a:1,b:0,r:1000});render();}else get('red-result').textContent='Máximo ocho resistencias en este taller.';});
  get('red-series').addEventListener('click',()=>{parts=[{a:1,b:2,r:220},{a:2,b:0,r:330}];render();});
  get('red-parallel').addEventListener('click',()=>{parts=[{a:1,b:0,r:220},{a:1,b:0,r:330}];render();});
  get('red-clear').addEventListener('click',()=>{parts=[];render();});
  const status = get('red-save-status');
  get('red-save').addEventListener('click', async () => {
    const rawVoltage=get('red-voltage').value.trim();
    const voltage = rawVoltage === '' ? NaN : Number(rawVoltage);
    if (!solve(voltage, parts)) { status.textContent='Corrige los valores antes de guardar.'; return; }
    try { await window.LabDesigns.request('CIRCUITO',{voltage,parts}); status.textContent='Diseño guardado en tu cuenta.'; }
    catch (error) { status.textContent=error.message; }
  });
  get('red-load').addEventListener('click', async () => {
    try { const saved=await window.LabDesigns.request('CIRCUITO'); if (!solve(saved.voltage,saved.parts)) throw new Error('El diseño guardado no es válido.'); parts=saved.parts;get('red-voltage').value=saved.voltage;render();status.textContent='Diseño recuperado.'; }
    catch (error) { status.textContent=error.message; }
  });
  render();
})();
