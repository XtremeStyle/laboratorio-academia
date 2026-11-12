-- Catálogo de cursos y seguimiento académico. No altera ni elimina datos previos.
CREATE TABLE curso (
    id UUID PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    nivel VARCHAR(40) NOT NULL,
    duracion_minutos INTEGER NOT NULL CHECK (duracion_minutos > 0),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PUBLICADO','BORRADOR','ARCHIVADO')),
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE curso_semillero (
    curso_id UUID NOT NULL REFERENCES curso(id),
    semillero_id UUID NOT NULL REFERENCES semillero(id),
    PRIMARY KEY (curso_id, semillero_id)
);
CREATE TABLE leccion_curso (
    id UUID PRIMARY KEY,
    curso_id UUID NOT NULL REFERENCES curso(id),
    numero INTEGER NOT NULL CHECK (numero > 0),
    titulo VARCHAR(200) NOT NULL,
    objetivo TEXT NOT NULL,
    contenido TEXT NOT NULL,
    actividad TEXT NOT NULL,
    evidencia TEXT NOT NULL,
    duracion_minutos INTEGER NOT NULL CHECK (duracion_minutos > 0),
    obligatoria BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (curso_id, numero),
    UNIQUE (id, curso_id)
);
CREATE TABLE matricula_curso (
    id UUID PRIMARY KEY,
    curso_id UUID NOT NULL REFERENCES curso(id),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    semillero_id UUID NOT NULL REFERENCES semillero(id),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('ACTIVA','COMPLETADA','RETIRADA')),
    matriculada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (curso_id, usuario_id, semillero_id),
    UNIQUE (id, curso_id),
    FOREIGN KEY (curso_id, semillero_id) REFERENCES curso_semillero(curso_id, semillero_id)
);
CREATE TABLE progreso_leccion (
    id UUID PRIMARY KEY,
    matricula_id UUID NOT NULL,
    curso_id UUID NOT NULL,
    leccion_id UUID NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('NO_INICIADA','EN_CURSO','COMPLETADA')),
    evidencia TEXT,
    iniciado_en TIMESTAMP WITH TIME ZONE,
    completado_en TIMESTAMP WITH TIME ZONE,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (matricula_id, curso_id) REFERENCES matricula_curso(id, curso_id),
    FOREIGN KEY (leccion_id, curso_id) REFERENCES leccion_curso(id, curso_id),
    UNIQUE (matricula_id, leccion_id),
    CHECK (estado <> 'COMPLETADA' OR completado_en IS NOT NULL),
    CHECK (estado = 'NO_INICIADA' OR iniciado_en IS NOT NULL)
);
CREATE INDEX ix_curso_semillero ON curso_semillero(semillero_id, curso_id);
CREATE INDEX ix_matricula_usuario ON matricula_curso(usuario_id, estado);
CREATE INDEX ix_progreso_matricula ON progreso_leccion(matricula_id, estado);

INSERT INTO curso(id,codigo,titulo,descripcion,nivel,duracion_minutos,estado) VALUES
('c0000000-0000-4000-8000-000000000001','ROBOTICA-INICIAL','De la corriente al primer prototipo','Ruta práctica de electricidad, electrónica, programación física, sensores y robótica móvil.','INICIAL',495,'PUBLICADO'),
('c0000000-0000-4000-8000-000000000002','COSMOS-INICIAL','Explorar el cielo y diseñar una misión','Ruta de astronomía observacional, gravitación, cohetería educativa segura, aerodinámica y telemetría.','INICIAL',480,'PUBLICADO');

INSERT INTO leccion_curso(id,curso_id,numero,titulo,objetivo,contenido,actividad,evidencia,duracion_minutos) VALUES
('c1000000-0000-4000-8000-000000000001','c0000000-0000-4000-8000-000000000001',1,'Electricidad sin misterio','Distinguir voltaje, corriente y resistencia y aplicar la ley de Ohm.','Magnitudes eléctricas, unidades, continuidad, ley de Ohm y lectura de diagramas.','Medir una pila, comprobar continuidad y calcular la resistencia para un LED.','Ficha de mediciones, cálculos y esquema del circuito.',60),
('c1000000-0000-4000-8000-000000000002','c0000000-0000-4000-8000-000000000001',2,'Protoboard y conexiones','Montar un circuito sencillo con polaridad y conexiones correctas.','Rieles, filas, puentes, cables Dupont, LED y resistencia limitadora.','Montar y documentar un circuito LED en protoboard.','Fotografía o diagrama anotado del montaje.',75),
('c1000000-0000-4000-8000-000000000003','c0000000-0000-4000-8000-000000000001',3,'Microcontroladores','Controlar entradas y salidas digitales de una placa educativa.','Arduino-compatible y ESP32, GPIO, entradas, salidas y PWM.','Programar un LED con una salida digital y documentar pines.','Código comentado y tabla de conexiones.',90),
('c1000000-0000-4000-8000-000000000004','c0000000-0000-4000-8000-000000000001',4,'El robot percibe','Interpretar señales de sensores y calibrar lecturas.','Sensores LDR, temperatura y distancia, señales analógicas y digitales.','Registrar lecturas y activar una alerta al superar un umbral.','Tabla de datos, calibración y conclusión.',90),
('c1000000-0000-4000-8000-000000000005','c0000000-0000-4000-8000-000000000001',5,'Movimiento y energía','Controlar un actuador respetando límites eléctricos.','Servomotores, drivers, alimentación separada y protección.','Controlar un servo y dibujar su esquema de alimentación.','Diagrama y registro de pruebas.',90),
('c1000000-0000-4000-8000-000000000006','c0000000-0000-4000-8000-000000000001',6,'Robot explorador','Integrar sensor, controlador y actuador en un prototipo.','Diseño, integración, depuración, pruebas y bitácora técnica.','Diseñar un robot que detecte obstáculos y responda, simular o construir a baja tensión.','Prototipo o simulación, código, esquema y bitácora.',90),
('c1000000-0000-4000-8000-000000000007','c0000000-0000-4000-8000-000000000002',1,'Cartografiar el cielo','Reconocer referencias y movimientos aparentes del cielo.','Esfera celeste, coordenadas, orientación, fases y observación responsable.','Planificar y registrar observaciones durante siete noches.','Diario de observación con fecha, hora y condiciones.',75),
('c1000000-0000-4000-8000-000000000008','c0000000-0000-4000-8000-000000000002',2,'Gravedad y órbitas','Relacionar las leyes de Kepler con el modelo gravitacional.','Gravitación de Newton, periodo, excentricidad, perihelio y afelio.','Explorar órbitas con el simulador y comparar parámetros.','Tabla de parámetros y explicación de resultados.',90),
('c1000000-0000-4000-8000-000000000009','c0000000-0000-4000-8000-000000000002',3,'Cohetería educativa segura','Explicar impulso, estabilidad y centro de masa en modelos no propulsados.','Acción-reacción, impulso, masa, estabilidad y centro de presión.','Diseñar y probar un modelo de papel sin propulsión ni lanzamiento motorizado.','Planos, cálculos y registro de pruebas manuales.',75),
('c1000000-0000-4000-8000-000000000010','c0000000-0000-4000-8000-000000000002',4,'Aerodinámica','Describir cómo el área y la forma afectan el arrastre.','Resistencia del aire, área frontal, forma y límites de modelos ideales.','Comparar diseños de papel mediante una prueba segura de caída manual.','Tabla comparativa y conclusión.',75),
('c1000000-0000-4000-8000-000000000011','c0000000-0000-4000-8000-000000000002',5,'Telemetría y datos','Interpretar mediciones de sensores en una misión.','Altitud, aceleración, presión, muestreo, ruido y visualización de datos.','Analizar un conjunto de datos simulado de una misión.','Gráfica, unidades y análisis de incertidumbre.',75),
('c1000000-0000-4000-8000-000000000012','c0000000-0000-4000-8000-000000000002',6,'Misión Cosmos','Integrar ciencia, diseño y comunicación en una propuesta verificable.','Dossier técnico, simulación de trayectoria, seguridad y comunicación científica.','Diseñar una misión simulada con objetivos, restricciones y plan de medición.','Dossier, simulación, póster y bitácora.',90);
