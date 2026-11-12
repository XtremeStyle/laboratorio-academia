package com.laboratorio.persistence;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class AssignmentTrackMigrationTest {
    @Test void backfillsExistingAssignmentsBeforeEnforcingTrackIntegrity() {
        String url="jdbc:h2:mem:migration_"+UUID.randomUUID().toString().replace("-","")+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration").target("4").load().migrate();
        var db=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
        UUID academy=UUID.randomUUID(),user=UUID.randomUUID(),track=UUID.randomUUID(),mission=UUID.randomUUID(),assignment=UUID.randomUUID();
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'A','ACTIVA')",academy);
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'migration@example.test','A','B','hash','ACTIVO')",user,academy);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'MIG','Migración','ACTIVO')",track,academy);
        db.update("INSERT INTO mision(id,academia_id,titulo,instrucciones,modalidad,estado,creada_por_id,politica_evaluacion) VALUES (?,?,'M','I','INDIVIDUAL','PUBLICADA',?,'POR_ETAPA')",mission,academy,user);
        db.update("INSERT INTO mision_semillero(id,academia_id,mision_id,semillero_id) VALUES (?,?,?,?)",UUID.randomUUID(),academy,mission,track);
        db.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,destinatario_usuario_id,estado) VALUES (?,?,?,?,'ACTIVA')",assignment,academy,mission,user);
        Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration").load().migrate();
        assertEquals(track,db.queryForObject("SELECT semillero_destino_id FROM asignacion_mision WHERE id=?",UUID.class,assignment));
        UUID alienTrack=UUID.randomUUID(); db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ALIEN','Otro','ACTIVO')",alienTrack,academy);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("UPDATE asignacion_mision SET semillero_destino_id=? WHERE id=?",alienTrack,assignment));
    }
}
