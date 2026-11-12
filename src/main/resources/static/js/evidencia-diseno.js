(function () {
  'use strict';
  document.querySelectorAll('.import-design').forEach(button => {
    button.addEventListener('click', async () => {
      const form=button.closest('form'), input=form.querySelector('textarea'), status=form.querySelector('.import-design-status');
      try {
        const saved=await window.LabDesigns.request(button.dataset.type);
        const summary=button.dataset.type === 'CIRCUITO'
          ? `Diseño de circuito: batería de ${saved.voltage} V; ` + (saved.parts.length ? saved.parts.map((p,i)=>`R${i+1}: ${['−','+','A','B'][p.a]} a ${['−','+','A','B'][p.b]}, ${p.r} Ω`).join('; ') : 'sin resistencias') + '.'
          : `Diseño de cohete de papel no propulsado: cuerpo de ${saved.height} cm, punta ${saved.nose === 'cone' ? 'cónica' : 'redondeada'}, ${saved.fins} aletas; paso del taller ${saved.stage+1} de 5.`;
        const next = input.value.trim() ? input.value.trim() + '\n\n' + summary : summary;
        if(next.length > 5000) { status.textContent='El diseño supera el espacio disponible en esta evidencia.'; return; }
        input.value=next; status.textContent='Diseño añadido. Revisa el texto y guarda tu avance o completa la lección.';
      } catch(error) { status.textContent=error.message; }
    });
  });
})();
