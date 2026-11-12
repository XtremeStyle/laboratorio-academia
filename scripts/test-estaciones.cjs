const assert = require('node:assert/strict');
const {signalSeconds, robotStopped} = require('../src/main/resources/static/js/estaciones.js');
assert.ok(Math.abs(signalSeconds(1) - 499.0048) < 0.1);
assert.ok(Math.abs(signalSeconds(5.2) / signalSeconds(1) - 5.2) < 1e-9);
assert.equal(signalSeconds(-1), null);
assert.equal(robotStopped(30, 30), true);
assert.equal(robotStopped(31, 30), false);
console.log('Estaciones interactivas: comprobaciones aprobadas.');
