/* Prueba del modelo de lanzamiento educativo: empuje, quemado, TWR y Δv. */
const { solve } = require('../src/main/resources/static/js/lanzamiento.js');
const assert = require('assert');

// Caso didáctico: esqueleto 60 g, propelente 20 g, empuje 18 N, ve = 800 m/s
const r = solve(0.06, 0.02, 18, 800);
assert(r, 'el modelo debe resolver con entradas válidas');

// ṁ = F / vₑ
assert.strictEqual(Math.round(r.massFlow * 1e6), 22500, 'ṁ = F/vₑ = 22,5 g/s');
// t = m_p / ṁ
assert(Math.abs(r.burnTime - 0.02 / 0.0225) < 1e-9, 'tiempo de quemado = m_p/ṁ ≈ 0,889 s');
// a = F/m − g
assert(Math.abs(r.acceleration - (18 / 0.08 - 9.80665)) < 1e-9, 'aceleración correcta');
// Δv = ve·ln(m0/me)
assert(Math.abs(r.deltaV - 800 * Math.log(0.08 / 0.06)) < 1e-9, 'Δv Tsiolkovsky correcto');
// TWR = F/(m·g)
assert(Math.abs(r.twr - 18 / (0.08 * 9.80665)) < 1e-9, 'TWR correcto');
// Altura = quemado + ascenso libre
assert(r.maxAltitude > r.burnDistance, 'la altura total supera la de quemado');
assert(r.takesOff, 'con TWR > 1 el cohete despega');

// Caso sin despegue: empuje menor que el peso
const sinDespegue = solve(0.06, 0.02, 0.5, 800);
assert.strictEqual(sinDespegue.takesOff, false, 'empuje insuficiente no despega');

// Entrada inválida → null
assert.strictEqual(solve(0, 0.02, 18, 800), null, 'masa nula rechazada');
assert.strictEqual(solve(0.06, -1, 18, 800), null, 'propelente negativo rechazado');

console.log('OK · modelo de lanzamiento verificado');
console.log(`   tiempo de quemado ${r.burnTime.toFixed(3)} s · Δv ${r.deltaV.toFixed(1)} m/s · TWR ${r.twr.toFixed(2)} · altura ${r.maxAltitude.toFixed(2)} m`);
