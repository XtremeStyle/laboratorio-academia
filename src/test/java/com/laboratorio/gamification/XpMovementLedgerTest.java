package com.laboratorio.gamification;

import com.laboratorio.evaluation.EvaluationService;
import com.laboratorio.mission.MissionService;
import com.laboratorio.submission.SubmissionService;
import com.laboratorio.crew.CrewService;
import com.laboratorio.research.Assignments;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class XpMovementLedgerTest {
    @Autowired JdbcTemplate db;
    @Autowired MissionService missions;
    @Autowired Assignments assignments;
    @Autowired SubmissionService submissions;
    @Autowired EvaluationService evaluations;
    @Autowired XpMovementService xp;
    @Autowired RankingService ranking;
    @Autowired CrewService crews;

    UUID academy = UUID.randomUUID(), track = UUID.randomUUID();
    UUID admin = UUID.randomUUID(), teacher = UUID.randomUUID(), student1 = UUID.randomUUID(), student2 = UUID.randomUUID();

    private void setup() {
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'XP','ACTIVA')", academy);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'XP1','XP','ACTIVO')", track, academy);
        for (Object[] entry : List.of(new Object[]{admin, "admin-xp@example.test", "ADMINISTRADOR"}, new Object[]{teacher, "teacher-xp@example.test", "PROFESOR"}, new Object[]{student1, "s1-xp@example.test", "ESTUDIANTE"}, new Object[]{student2, "s2-xp@example.test", "ESTUDIANTE"})) {
            UUID id = (UUID) entry[0]; String email = (String) entry[1]; String role = (String) entry[2];
            db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Nombre','Prueba','hash','ACTIVO')", id, academy, email);
            db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo=?", UUID.randomUUID(), id, role);
        }
        assignments.assign("admin-xp@example.test", teacher, track);
        assignments.assign("admin-xp@example.test", student1, track);
        assignments.assign("admin-xp@example.test", student2, track);
    }

    private String createMission(String email, String titulo, String modalidad) {
        missions.create(email, track, titulo, "Instrucciones de la Misión", "Criterio principal", modalidad, LocalDate.now().plusDays(10).toString(), List.of());
        return titulo;
    }

    @Test void evalOtorgaYCorreccionAjustaYPanelCoincide() {
        setup();
        createMission("teacher-xp@example.test", "Misión XP individual", "INDIVIDUAL");
        UUID mission = db.queryForObject("SELECT id FROM mision WHERE titulo='Misión XP individual'", UUID.class);
        missions.assign("teacher-xp@example.test", mission, student1, null, "");
        UUID assignment = db.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?", UUID.class, mission);
        submissions.send("s1-xp@example.test", assignment, "Entrega de texto válida para la etapa final.");
        UUID delivery = db.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?", UUID.class, assignment);
        evaluations.grade("teacher-xp@example.test", delivery, "5", "Excelente", Map.of());
        Integer saldo = db.queryForObject("SELECT saldo FROM xp_movimiento WHERE usuario_id=? AND cantidad=100", Integer.class, student1);
        assertEquals(100, saldo);
        Integer suma = db.queryForObject("SELECT SUM(cantidad) FROM xp_movimiento WHERE usuario_id=?", Integer.class, student1);
        assertEquals(100, suma);
        assertEquals(100, xp.totalFor("s1-xp@example.test"));
        assertEquals(100, ranking.personal("s1-xp@example.test").xp());
        // Corrección: 2,5 → ajuste delta −50, el saldo queda 50 y el panel coincide
        evaluations.grade("teacher-xp@example.test", delivery, "2.5", "Corrección", Map.of());
        Integer count = db.queryForObject("SELECT COUNT(*) FROM xp_movimiento WHERE usuario_id=?", Integer.class, student1);
        assertEquals(2, count);
        Integer sumaTotal = db.queryForObject("SELECT SUM(cantidad) FROM xp_movimiento WHERE usuario_id=?", Integer.class, student1);
        assertEquals(50, sumaTotal);
        Integer cantidad = db.queryForObject("SELECT cantidad FROM xp_movimiento WHERE usuario_id=? AND ABS(cantidad)=50", Integer.class, student1);
        assertEquals(-50, cantidad);
        assertEquals(50, xp.totalFor("s1-xp@example.test"));
        assertEquals(50, ranking.personal("s1-xp@example.test").xp());
    }

    @Test void grupalAjusteIndividualOtorgaGamificacionPorIntegrante() {
        setup();
        missions.create("teacher-xp@example.test", track, "Misión XP grupal", "Instrucciones", "Criterio", "GRUPAL", LocalDate.now().plusDays(10).toString(), List.of());
        UUID mission = db.queryForObject("SELECT id FROM mision WHERE titulo='Misión XP grupal'", UUID.class);
        crews.create("admin-xp@example.test", track, "XPNova");
        UUID crew = db.queryForObject("SELECT id FROM tripulacion WHERE academia_id=? AND nombre='XPNova'", UUID.class, academy);
        crews.add("admin-xp@example.test", crew, student1, "PARTICIPANTE");
        crews.add("admin-xp@example.test", crew, student2, "PARTICIPANTE");
        missions.assign("teacher-xp@example.test", mission, crew, null, "");
        UUID assignment = db.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?", UUID.class, mission);
        submissions.send("s1-xp@example.test", assignment, "Entrega grupal con proceso y resultados compartidos.");
        UUID delivery = db.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?", UUID.class, assignment);
        evaluations.grade("teacher-xp@example.test", delivery, "4", "Buen trabajo", Map.of("ajuste_" + student2, "-1.5", "justificacion_" + student2, "No aportó al reparto"));
        assertEquals(80, xp.totalFor("s1-xp@example.test"), "integrante sin ajuste recibe 4,0 → 80 XP");
        assertEquals(50, xp.totalFor("s2-xp@example.test"), "integrante con ajuste −1,5 recibe 2,5 → 50 XP");
        assertEquals(80, ranking.personal("s1-xp@example.test").xp());
        assertEquals(50, ranking.personal("s2-xp@example.test").xp());
    }
}
