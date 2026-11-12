(function () {
  'use strict';
  document.querySelectorAll('.lesson-check').forEach(activity => {
    const button = activity.querySelector('.check-answer');
    const feedback = activity.querySelector('.check-feedback');
    button.addEventListener('click', () => {
      const answer = activity.querySelector('input[type="radio"]:checked');
      if (!answer) { feedback.textContent = 'Selecciona una respuesta para continuar.'; return; }
      feedback.textContent = answer.value === button.dataset.answer
        ? '¡Correcto! ' + button.dataset.explanation
        : 'Inténtalo de nuevo. Revisa la explicación y vuelve a elegir.';
    });
  });
})();
