(() => {
  const panel = document.querySelector('#chat');
  if (!panel) return;
  const status = document.querySelector('#chat-status');
  const list = document.querySelector('#chat-messages');
  const form = document.querySelector('#chat-form');
  const input = document.querySelector('#chat-text');
  const url = new URL(panel.dataset.socket, window.location.href);
  url.protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const socket = new WebSocket(url);
  function append(message) {
    const item = document.createElement('li');
    const author = document.createElement('strong'); author.textContent = message.author;
    const time = document.createElement('time'); time.textContent = message.sent;
    const text = document.createElement('p'); text.textContent = message.content;
    item.append(author, document.createTextNode(' · '), time, text); list.append(item);
  }
  socket.addEventListener('open', () => { status.textContent = 'Conectado'; });
  socket.addEventListener('close', () => { status.textContent = 'Conexión cerrada. Recarga la página para volver a conectar.'; });
  socket.addEventListener('message', event => {
    const data = JSON.parse(event.data);
    if (data.type === 'history') { list.replaceChildren(); data.messages.forEach(append); }
    if (data.type === 'message') append(data.message);
    if (data.type === 'error') status.textContent = data.message;
  });
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (socket.readyState !== WebSocket.OPEN) { status.textContent = 'No hay conexión.'; return; }
    if (!input.value.trim()) return;
    socket.send(input.value); input.value = '';
  });
})();
