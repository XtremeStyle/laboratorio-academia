(function () {
  'use strict';
  if (typeof document === 'undefined') return;
  const get=id=>document.getElementById(id), root=get('taller-cohete'); if(!root)return;
  const body=get('rocket-body'), fins=get('rocket-fins'), nose=get('rocket-nose'), stage=get('rocket-stage');
  const descriptions = [
    ['1. Define tu misión','Imagina qué deseas observar. Describe tu objetivo en una bitácora antes de elegir la forma.'],
    ['2. Elige el cuerpo','Cambia la longitud y observa cómo se modifica el espacio disponible en tu modelo de papel.'],
    ['3. Diseña la nariz','Compara una punta cónica con una redondeada. Observa la forma, sin asumir un resultado aerodinámico real.'],
    ['4. Organiza las aletas','Cambia el número de aletas y considera cómo distribuirlas de manera simétrica.'],
    ['5. Revisa y documenta','Comprueba que el modelo no tiene propulsión y registra tus decisiones y una posible mejora.']
  ];
  function update(){
    const height=Number(body.value), count=Number(fins.value), top=nose.value, step=Number(stage.value);
    get('rocket-body-value').textContent=`${height} cm`;get('rocket-fins-value').textContent=`${count} aletas`;
    get('rocket-shape').setAttribute('height',height*4);get('rocket-shape').setAttribute('y',220-height*4);
    const y=220-height*4;get('rocket-tip').setAttribute('d',top==='cone'?`M 150 ${y} L 180 ${y-45} L 210 ${y} Z`:`M 150 ${y} Q 180 ${y-50} 210 ${y} Z`);
    get('rocket-left-fin').setAttribute('d',`M 150 180 L 120 220 L 150 220 Z`);get('rocket-right-fin').setAttribute('d',`M 210 180 L 240 220 L 210 220 Z`);
    get('rocket-center-fin').style.display=count===4?'':'none';
    get('rocket-step-title').textContent=descriptions[step][0];get('rocket-step-text').textContent=descriptions[step][1];
    get('rocket-summary').textContent=`Diseño: cuerpo de ${height} cm, punta ${top==='cone'?'cónica':'redondeada'} y ${count} aletas. Modelo no propulsado.`;
    get('rocket-progress').value=step+1;
  }
  [body,fins,nose,stage].forEach(x=>x.addEventListener('input',update));
  get('rocket-prev').addEventListener('click',()=>{stage.value=Math.max(0,Number(stage.value)-1);update();});
  get('rocket-next').addEventListener('click',()=>{stage.value=Math.min(4,Number(stage.value)+1);update();});
  const status=get('rocket-save-status');
  get('rocket-save').addEventListener('click',async()=>{
    try { await window.LabDesigns.request('COHETE',{height:Number(body.value),fins:Number(fins.value),nose:nose.value,stage:Number(stage.value)}); status.textContent='Diseño guardado en tu cuenta.'; }
    catch(error) { status.textContent=error.message; }
  });
  get('rocket-load').addEventListener('click',async()=>{
    try { const saved=await window.LabDesigns.request('COHETE');body.value=saved.height;fins.value=saved.fins;nose.value=saved.nose;stage.value=saved.stage;update();status.textContent='Diseño recuperado.'; }
    catch(error) { status.textContent=error.message; }
  });
  update();
})();
