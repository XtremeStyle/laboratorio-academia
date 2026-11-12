/* Prueba del modelo de fases lunares: iluminación en puntos de referencia y nombres. */
const { illumination, phaseName, SINODIC } = require('../src/main/resources/static/js/fases-luna.js');
const assert = require('assert');

const approx = (a, b, tol) => Math.abs(a - b) <= (tol == null ? 1e-4 : tol);
assert(approx(illumination(0), 0), 'luna nueva: 0% iluminada');
assert(approx(illumination(SINODIC / 4), 0.5, 0.01), 'cuarto creciente: 50% iluminada');
assert(approx(illumination(SINODIC / 2), 1), 'luna llena: 100% iluminada');
assert(approx(illumination((3 * SINODIC) / 4), 0.5, 0.01), 'cuarto menguante: 50% iluminada');

assert.strictEqual(phaseName(0), 'Luna nueva');
assert.strictEqual(phaseName(SINODIC / 2), 'Luna llena');

console.log('OK · fases de la Luna: iluminación y nombres de fase verificados');
