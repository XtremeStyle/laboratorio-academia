package com.laboratorio.persistence;

import com.laboratorio.evaluation.EvaluationService;
import com.laboratorio.mission.MissionService;
import com.laboratorio.research.Assignments;
import com.laboratorio.submission.SubmissionService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:concurrency_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {
    @Autowired JdbcTemplate db; @Autowired Assignments assignments; @Autowired MissionService missions; @Autowired SubmissionService submissions; @Autowired EvaluationService evaluations;
    @Test void concurrentEnrollmentAndEvaluationsKeepOneActiveAssignmentAndOrderedVersions() throws Exception {
        UUID academy=UUID.randomUUID(),admin=UUID.randomUUID(),student=UUID.randomUUID(),track=UUID.randomUUID();
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'Concurrencia','ACTIVA')",academy);
        for(var entry:Map.of(admin,"admin-concurrency@example.test",student,"student-concurrency@example.test").entrySet()) db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Nombre','Prueba','hash','ACTIVO')",entry.getKey(),academy,entry.getValue());
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),admin);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ESTUDIANTE'",UUID.randomUUID(),student);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'CON','Concurrencia','ACTIVO')",track,academy);
        var pool=Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start=new CountDownLatch(1);
            Callable<Boolean> enroll=()->{ start.await(); try { assignments.assign("admin-concurrency@example.test",student,track); return true; } catch(IllegalArgumentException ex) { return false; } };
            var a=pool.submit(enroll); var b=pool.submit(enroll); start.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM asignacion_semillero x JOIN asignacion_activa aa ON aa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=?",Integer.class,student,track));
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM periodo_asignacion p JOIN asignacion_semillero x ON x.id=p.asignacion_id WHERE x.usuario_id=? AND x.semillero_id=?",Integer.class,student,track));
            missions.create("admin-concurrency@example.test",track,"Misión concurrente","Ejercicio","Claridad","INDIVIDUAL",LocalDate.now().plusDays(5).toString(),List.of());
            UUID mission=db.queryForObject("SELECT id FROM mision WHERE titulo='Misión concurrente'",UUID.class);
            missions.assign("admin-concurrency@example.test",mission,student,null,"");
            UUID assignment=db.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?",UUID.class,mission);
            submissions.send("student-concurrency@example.test",assignment,"Entrega");
            UUID delivery=db.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?",UUID.class,assignment);
            CountDownLatch gradeStart=new CountDownLatch(1);
            Callable<Void> grade=()->{ gradeStart.await(); evaluations.grade("admin-concurrency@example.test",delivery,"4","",Map.of()); return null; };
            var first=pool.submit(grade); var second=pool.submit(grade); gradeStart.countDown(); first.get(15,TimeUnit.SECONDS); second.get(15,TimeUnit.SECONDS);
            assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM version_evaluacion WHERE entrega_etapa_id=?",Integer.class,delivery));
            assertEquals(List.of(1,2),db.queryForList("SELECT numero FROM version_evaluacion WHERE entrega_etapa_id=? ORDER BY numero",Integer.class,delivery));
        } finally { pool.shutdownNow(); }
    }
}
