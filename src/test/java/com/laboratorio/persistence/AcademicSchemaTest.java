package com.laboratorio.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcademicSchemaTest {
    @Autowired JdbcTemplate db;
    UUID academy = UUID.randomUUID(), otherAcademy = UUID.randomUUID();
    UUID user = UUID.randomUUID(), otherUser = UUID.randomUUID(), track = UUID.randomUUID();

    @BeforeEach
    void setup() {
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')", academy, "Academia");
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')", otherAcademy, "Otra");
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'A','B','hash','ACTIVO')", user, academy, "a@example.test");
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'C','D','hash','ACTIVO')", otherUser, otherAcademy, "b@example.test");
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,?,'Robótica','ACTIVO')", track, academy, "ROB");
    }

    @Test void seedsRolesAndEnforcesGlobalNormalizedEmail() {
        assertEquals(3, db.queryForObject("SELECT COUNT(*) FROM rol", Integer.class));
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'X','Y','hash','ACTIVO')", UUID.randomUUID(), otherAcademy, "a@example.test"));
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'X','Y','hash','ACTIVO')", UUID.randomUUID(), academy, "MAYUS@example.test"));
    }

    @Test void preventsCrossAcademyMembershipAndDuplicateActiveAssignment() {
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)", UUID.randomUUID(), academy, otherUser, track));
        UUID assignment = UUID.randomUUID(), period = UUID.randomUUID();
        db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)", assignment, academy, user, track);
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)", period, assignment, user);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)", assignment, period);
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)", assignment, period));
    }

    @Test void rejectsAssignmentWithNoRecipientAndStagesFromAnotherMission() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), assignment = UUID.randomUUID(), stage = UUID.randomUUID();
        for (UUID mission : new UUID[]{first, second}) {
            db.update("INSERT INTO mision(id,academia_id,titulo,instrucciones,modalidad,estado,creada_por_id,politica_evaluacion) VALUES (?,?,'M','I','INDIVIDUAL','BORRADOR',?,'SOLO_FINAL')", mission, academy, user);
            db.update("INSERT INTO mision_semillero(id,academia_id,mision_id,semillero_id) VALUES (?,?,?,?)",UUID.randomUUID(),academy,mission,track);
        }
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,semillero_destino_id,estado) VALUES (?,?,?,?,'ACTIVA')", UUID.randomUUID(), academy, first,track));
        db.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,destinatario_usuario_id,semillero_destino_id,estado) VALUES (?,?,?,?,?,'ACTIVA')", assignment, academy, first, user,track);
        db.update("INSERT INTO etapa_mision(id,mision_id,titulo,orden,obligatoria,es_final) VALUES (?,?,'Final',1,TRUE,TRUE)", stage, second);
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO entrega_etapa(id,mision_id,asignacion_id,etapa_id) VALUES (?,?,?,?)", UUID.randomUUID(), first, assignment, stage));
    }

    @Test void keepsVersionNumbersUniqueAndRejectsCrossDeliveryEvaluation() {
        UUID mission = UUID.randomUUID(), assignment = UUID.randomUUID(), stage1 = UUID.randomUUID(), stage2 = UUID.randomUUID();
        UUID delivery1 = UUID.randomUUID(), delivery2 = UUID.randomUUID(), version = UUID.randomUUID(), evaluation = UUID.randomUUID();
        db.update("INSERT INTO mision(id,academia_id,titulo,instrucciones,modalidad,estado,creada_por_id,politica_evaluacion) VALUES (?,?,'M','I','INDIVIDUAL','BORRADOR',?,'POR_ETAPA')", mission, academy, user);
        db.update("INSERT INTO mision_semillero(id,academia_id,mision_id,semillero_id) VALUES (?,?,?,?)",UUID.randomUUID(),academy,mission,track);
        db.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,destinatario_usuario_id,semillero_destino_id,estado) VALUES (?,?,?,?,?,'ACTIVA')", assignment, academy, mission, user,track);
        db.update("INSERT INTO etapa_mision(id,mision_id,titulo,orden,obligatoria,es_final) VALUES (?,?,'Uno',1,TRUE,FALSE)", stage1, mission);
        db.update("INSERT INTO etapa_mision(id,mision_id,titulo,orden,obligatoria,es_final) VALUES (?,?,'Final',2,TRUE,TRUE)", stage2, mission);
        db.update("INSERT INTO entrega_etapa(id,mision_id,asignacion_id,etapa_id) VALUES (?,?,?,?)", delivery1, mission, assignment, stage1);
        db.update("INSERT INTO entrega_etapa(id,mision_id,asignacion_id,etapa_id) VALUES (?,?,?,?)", delivery2, mission, assignment, stage2);
        db.update("INSERT INTO version_entrega(id,entrega_etapa_id,numero,autor_id,contenido,enviada_en) VALUES (?,?,1,?,'texto',CURRENT_TIMESTAMP)", version, delivery1, user);
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO version_entrega(id,entrega_etapa_id,numero,autor_id,contenido,enviada_en) VALUES (?,?,1,?,'texto',CURRENT_TIMESTAMP)", UUID.randomUUID(), delivery1, user));
        db.update("INSERT INTO evaluacion(id,entrega_etapa_id) VALUES (?,?)", evaluation, delivery2);
        assertThrows(DataIntegrityViolationException.class, () -> db.update("INSERT INTO version_evaluacion(id,evaluacion_id,entrega_etapa_id,numero,version_entrega_id,evaluador_id,calificacion_base,registrada_en) VALUES (?,?,?,1,?,?,5,CURRENT_TIMESTAMP)", UUID.randomUUID(), evaluation, delivery2, version, user));
    }
}
