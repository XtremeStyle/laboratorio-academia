/* Prueba del modelo PWM: voltaje medio en función del ciclo de trabajo. */
const { averageVoltage } = require('../src/main/resources/static/js/pulsos.js');
const assert = require('assert');

assert.strictEqual(averageVoltage(50, 5), 2.5, '50% de 5 V = 2,5 V');
assert.strictEqual(averageVoltage(100, 5), 5, '100% = VCC');
assert.strictEqual(averageVoltage(0, 5), 0, '0% = 0 V');
assert.strictEqual(averageVoltage(25, 5), 1.25, '25% de 5 V');
assert.strictEqual(averageVoltage(150, 5), null, 'rechaza duty > 100');
console.log('OK · modelo PWM: voltaje medio verificado');
