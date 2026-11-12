package com.laboratorio.research;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Assignments {
    private final JdbcTemplate db;
    private final com.laboratorio.audit.AuditService audit;
    public Assignments(JdbcTemplate db,com.laboratorio.audit.AuditService audit) { this.db = db; this.audit=audit; }
    private UUID academy(String actor) {
        var result = db.query("SELECT u.academia_id FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'", (rs,i) -> rs.getObject(1, UUID.class), actor);
        if (result.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return result.getFirst();
    }
    @Transactional(readOnly = true)
    public View view(String actor) {
        UUID academy = academy(actor);
        var users = db.query("SELECT id,correo FROM usuario WHERE academia_id=? AND estado='ACTIVO' ORDER BY correo LIMIT 100", (rs,i) -> new Option(rs.getObject(1,UUID.class), rs.getString(2)), academy);
        var tracks = db.query("SELECT id,nombre FROM semillero WHERE academia_id=? AND estado='ACTIVO' ORDER BY nombre LIMIT 100", (rs,i) -> new Option(rs.getObject(1,UUID.class), rs.getString(2)), academy);
        var active = db.query("SELECT a.id,u.correo,s.nombre FROM asignacion_semillero a JOIN asignacion_activa aa ON aa.asignacion_id=a.id JOIN usuario u ON u.id=a.usuario_id JOIN semillero s ON s.id=a.semillero_id WHERE a.academia_id=? ORDER BY u.correo,s.nombre LIMIT 100", (rs,i) -> new Active(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)), academy);
        return new View(users, tracks, active);
    }
    @Transactional
    public void assign(String actor, UUID user, UUID track) {
        UUID academy = academy(actor);
        // La fila del usuario serializa altas concurrentes de la misma persona.
        var locked = db.query("SELECT id FROM usuario WHERE id=? AND academia_id=? AND estado='ACTIVO' FOR UPDATE", (rs,i) -> rs.getObject(1,UUID.class), user, academy);
        if (locked.isEmpty()) throw new IllegalArgumentException("Usuario o semillero no disponible en tu academia.");
        Integer trackCount = db.queryForObject("SELECT COUNT(*) FROM semillero WHERE id=? AND academia_id=? AND estado='ACTIVO'", Integer.class, track, academy);
        if (trackCount == null || trackCount != 1) throw new IllegalArgumentException("Usuario o semillero no disponible en tu academia.");
        UUID actorId = db.queryForObject("SELECT id FROM usuario WHERE correo=?", UUID.class, actor);
        var existing = db.query("SELECT id FROM asignacion_semillero WHERE usuario_id=? AND semillero_id=?", (rs,i) -> rs.getObject(1,UUID.class), user, track);
        UUID assignment;
        if (existing.isEmpty()) {
            assignment = UUID.randomUUID();
            db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)", assignment, academy, user, track);
        } else assignment = existing.getFirst();
        Integer active = db.queryForObject("SELECT COUNT(*) FROM asignacion_activa WHERE asignacion_id=?", Integer.class, assignment);
        if (active != null && active > 0) throw new IllegalArgumentException("La asignación ya está activa.");
        UUID period = UUID.randomUUID();
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,?,?)", period, assignment, OffsetDateTime.now(ZoneOffset.UTC), actorId);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)", assignment, period);
        audit.record(actorId,"ASIGNAR_SEMILLERO","ASIGNACION_SEMILLERO",assignment);
    }
    @Transactional
    public void close(String actor, UUID assignment) {
        UUID academy = academy(actor);
        var rows = db.query("SELECT a.usuario_id,aa.periodo_id FROM asignacion_semillero a JOIN asignacion_activa aa ON aa.asignacion_id=a.id WHERE a.id=? AND a.academia_id=?", (rs,i) -> new IdPair(rs.getObject(1,UUID.class), rs.getObject(2,UUID.class)), assignment, academy);
        if (rows.isEmpty()) throw new IllegalArgumentException("La asignación no está activa en tu academia.");
        var row = rows.getFirst();
        db.query("SELECT id FROM usuario WHERE id=? FOR UPDATE", (rs,i)->rs.getObject(1,UUID.class), row.user());
        Integer memberships = db.queryForObject("SELECT COUNT(*) FROM asignacion_semillero a JOIN tripulacion t ON t.semillero_id=a.semillero_id JOIN membresia_tripulacion m ON m.tripulacion_id=t.id AND m.usuario_id=a.usuario_id JOIN membresia_activa ma ON ma.membresia_id=m.id WHERE a.id=?", Integer.class, assignment);
        if (memberships != null && memberships > 0) throw new IllegalArgumentException("Primero cierra las membresías activas de este semillero.");
        int removed = db.update("DELETE FROM asignacion_activa WHERE asignacion_id=? AND periodo_id=?", assignment, row.period());
        if (removed != 1) throw new IllegalArgumentException("La asignación ya no está activa.");
        db.update("UPDATE periodo_asignacion SET fin=? WHERE id=? AND fin IS NULL", OffsetDateTime.now(ZoneOffset.UTC), row.period());
        audit.record(db.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor),"CERRAR_ASIGNACION_SEMILLERO","ASIGNACION_SEMILLERO",assignment);
    }
    public record Option(UUID id, String name) { }
    public record Active(UUID id, String email, String track) { }
    public record View(List<Option> users, List<Option> tracks, List<Active> active) { }
    private record IdPair(UUID user, UUID period) { }
}
