(function () {
  'use strict';
  async function request(type, data) {
    const token = document.querySelector('meta[name="_csrf"]');
    const name = document.querySelector('meta[name="_csrf_header"]');
    if (!token || !name) throw new Error('No se pudo verificar la sesión.');
    const headers = {}; headers[name.content] = token.content;
    if (data !== undefined) headers['Content-Type'] = 'application/json';
    const response = await fetch('/laboratorio/disenos/' + type, {
      method: data === undefined ? 'GET' : 'PUT', headers,
      ...(data === undefined ? {} : {body: JSON.stringify(data)})
    });
    if (response.status === 404 && data === undefined) throw new Error('Todavía no tienes un diseño guardado.');
    if (!response.ok || response.redirected) throw new Error('No se pudo completar la operación. Verifica tu sesión e inténtalo de nuevo.');
    return data === undefined ? response.json() : null;
  }
  window.LabDesigns = { request };
  window.dispatchEvent(new Event('lab-designs-ready'));
})();
