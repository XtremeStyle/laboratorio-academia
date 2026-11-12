# CHANGE Labs — guía técnica y pedagógica

## Laboratorio de Robótica y Electrónica

Ruta: `/laboratorio/circuitos`.

Simula un circuito en serie con batería, resistencia, interruptor y LED. El estudiante puede modificar tensión, resistencia, caída directa del LED, corriente máxima y potencia nominal. El modelo calcula corriente mediante la ley de Ohm y potencia disipada mediante Joule, representa el estado del interruptor y advierte cuando se exceden límites declarados.

**Alcance físico:** modelo DC ideal de un circuito resistivo en serie. No modela tolerancias, temperatura, curvas I-V, inductancia, capacitancia, PWM, transitorios ni fallas de cableado. No sustituye mediciones ni prácticas supervisadas con componentes reales.

## Laboratorio de Astronomía y Física Espacial

Ruta: `/laboratorio/astronomia`.

Simula un sistema de dos cuerpos con órbita kepleriana. Controles: masa de la estrella, semieje mayor, excentricidad y masa del planeta. Calcula periodo orbital (forma normalizada de la tercera ley de Kepler), distancias de perihelio y afelio, velocidad circular de referencia y velocidades en los extremos mediante la ecuación vis-viva en unidades astronómicas normalizadas. Incluye presets Tierra/Marte/órbita excéntrica, pausa, reinicio, velocidad de animación y retos de investigación.

**Alcance físico:** aproximación newtoniana de dos cuerpos, sin perturbaciones, relatividad, cuerpos adicionales, atmósfera ni integración numérica de efemérides. Los resultados son educativos y no aptos para navegación real.

## Pruebas locales

Desde `PROYECTO/`:

```bash
node scripts/test-circuitos.cjs
node scripts/test-astronomia.cjs
```

La prueba orbital valida un periodo terrestre aproximado, igualdad de perihelio/afelio en órbita circular, tendencia del periodo ante cambios de masa y distancia, y rechazo de excentricidades no elípticas.

## Seguridad y acceso

Ambas rutas requieren autenticación según la política general de la aplicación. Los recursos JS/CSS se sirven localmente. Las simulaciones se ejecutan en el navegador y no reciben ni almacenan datos personales.
