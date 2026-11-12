package com.laboratorio.evaluation;

import com.laboratorio.evaluation.EvaluationService;
import com.laboratorio.mission.MissionService;
import com.laboratorio.research.Assignments;
import com.laboratorio.submission.SubmissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Assertions;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class WeightedCriteriaTest {
    @Autowired JdbcTemplate db;
    @Autowired MissionService missions;
    @Autowired Assignments assignments;
    @Autowired SubmissionService submissions;
    @Autowired EvaluationService evaluations;

    UUID academy = UUID.randomUUID(), track = UUID.randomUUID(), admin = UUID.randomUUID(), student = UUID.randomUUID();
    UUID mission, stageDelivery;

    private void setup() {
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'W','ACTIVA')", academy);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'WD','Weighted','ACTIVO')", track, academy);
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'weighed@example.test','Ana','Prueba','HASH','ACTIVO')", admin, academy);
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'est-weighted@example.test','Bruno','Prueba','HASH','ACTIVO')", student, academy);
        for (Object[] r : List.of(new Object[]{admin, "ADMINISTRADOR"}, new Object[]{student, "ESTUDIANTE"})) {
            UUID id = (UUID) r[0]; String role = (String) r[1];
            db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo=?", UUID.randomUUID(), id, role);
        }
        assignments.assign("weighed@example.test", admin, track);
        assignments.assign("weighed@example.test", student, track);
        missions.create("weighed@example.test", track, "Misión ponderada", "Instrucciones", "Criterio base", "INDIVIDUAL", LocalDate.now().plusDays(10).toString(), List.of());
        mission = db.queryForObject("SELECT id FROM mision WHERE titulo='Misión ponderada'", UUID.class);
        UUID crit = db.queryForObject("SELECT id FROM criterio_evaluacion WHERE mision_id=?", UUID.class, mission);
        db.update("INSERT INTO competencia(id,academia_id,codigo,nombre,estado) VALUES (?,?,'COMP','Competencia ponderada','ACTIVA')", UUID.randomUUID(), academy);
        UUID comp = db.queryForObject("SELECT id FROM competencia WHERE codigo='COMP'", UUID.class);
        db.update("INSERT INTO competencia_semillero(id,competencia_id,semillero_id,academia_id) VALUES (?,?,?,?)", UUID.randomUUID(), comp, track, academy);
        db.update("INSERT INTO criterio_competencia(id,criterio_id,competencia_id) VALUES (?,?,?)", UUID.randomUUID(), crit, comp);
        missions.assign("weighed@example.test", mission, student, null, "");
        UUID assign = db.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?", UUID.class, mission);
        submissions.send("est-weighted@example.test", assign, "Entrega consolidada para prueba ponderada.");
        stageDelivery = db.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?", UUID.class, assign);
    }

    @Test void weightedCriteriaDriveTheEvaluation() {
        setup();
        UUID crit = db.queryForObject("SELECT id FROM criterio_evaluacion WHERE mision_id=?", UUID.class, mission);
        db.update("UPDATE criterio_evaluacion SET peso=2 WHERE id=?", crit);
        evaluations.grade("weighed@example.test", stageDelivery, "2.0", "Ponderado", Map.of("criterio_" + crit, "5"));
        BigDecimal stored = db.queryForObject("SELECT calificacion_base" + " FROM version_evaluacion v JOIN evaluacion e ON e.id=v.evaluacion_id JOIN entrega_etapa d ON d.id=e.entrega_etapa_id WHERE d.asignacion_id=(SELECT id FROM asignacion_mision WHERE mision_id=?)", BigDecimal.class, mission);
        Assertions.assertEquals(new BigDecimal("5.0000"), stored.setScale(4, java.math.RoundingMode.HALF_UP));
    }

    @Test void unweightedCriteriaFallsBackToManualGrade() {
        setup();
        UUID crit = db.queryForObject("SELECT id FROM criterio_evaluacion WHERE mision_id=?", UUID.class, mission);
        db.update("UPDATE criterio_evaluacion SET peso=NULL WHERE id=?", crit);
        evaluations.grade("weighed@example.test", stageDelivery, "3.0", "Manual", Map.of("criterio_" + crit, "5"));
        BigDecimal stored = db.queryForObject("SELECT calificacion_base" + " FROM version_evaluacion v JOIN evaluacion e ON e.id=v.evaluacion_id JOIN entrega_etapa d ON d.id=e.entrega_etapa_id WHERE d.asignacion_id=(SELECT id FROM asignacion_mision WHERE mision_id=?)", BigDecimal.class, mission);
        Assertions.assertEquals(new BigDecimal("3.0000"), stored.setScale(4, java.math.RoundingMode.HALF_UP));
    }

    }
