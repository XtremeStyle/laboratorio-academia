package com.laboratorio.crew;

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
public class CrewService {
    private final JdbcTemplate db;
    private final com.laboratorio.audit.AuditService audit;
    public CrewService(JdbcTemplate db,com.laboratorio.audit.AuditService audit) { this.db = db; this.audit=audit; }
    private UUID academy(String actor) {
        var r = db.query("SELECT u.academia_id FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'", (rs,i)->rs.getObject(1,UUID.class), actor);
        if (r.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return r.getFirst();
    }
    @Transactional(readOnly=true)
    public View view(String actor) {
        UUID academy = academy(actor);
        var tracks = db.query("SELECT id,nombre FROM semillero WHERE academia_id=? AND estado='ACTIVO' ORDER BY nombre LIMIT 100", (rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)), academy);
        var crews = db.query("SELECT t.id,t.nombre,s.nombre FROM tripulacion t JOIN semillero s ON s.id=t.semillero_id WHERE t.academia_id=? AND t.estado='ACTIVA' ORDER BY t.nombre LIMIT 100", (rs,i)->new Crew(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)), academy);
        var users = db.query("SELECT id,correo FROM usuario WHERE academia_id=? AND estado='ACTIVO' ORDER BY correo LIMIT 100", (rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)), academy);
        var members = db.query("SELECT m.id,u.correo,t.nombre,p.funcion FROM membresia_tripulacion m JOIN membresia_activa ma ON ma.membresia_id=m.id JOIN periodo_membresia p ON p.id=ma.periodo_id JOIN usuario u ON u.id=m.usuario_id JOIN tripulacion t ON t.id=m.tripulacion_id WHERE m.academia_id=? ORDER BY t.nombre,u.correo LIMIT 100", (rs,i)->new Member(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4)), academy);
        return new View(tracks,crews,users,members);
    }
    @Transactional
    public void create(String actor, UUID track, String name) {
        UUID academy = academy(actor);
        String clean = name.strip();
        if (clean.isEmpty() || clean.length() > 160) throw new IllegalArgumentException("El nombre debe tener de 1 a 160 caracteres.");
        Integer n = db.queryForObject("SELECT COUNT(*) FROM semillero WHERE id=? AND academia_id=? AND estado='ACTIVO'",Integer.class,track,academy);
        if (n == null || n != 1) throw new IllegalArgumentException("El semillero no está disponible en tu academia.");
        UUID crew=UUID.randomUUID();
        db.update("INSERT INTO tripulacion(id,academia_id,semillero_id,nombre,estado) VALUES (?,?,?,?,'ACTIVA')",crew,academy,track,clean);
        audit.record(db.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor),"CREAR_TRIPULACION","TRIPULACION",crew);
    }
    @Transactional
    public void add(String actor, UUID crew, UUID user, String function) {
        UUID academy = academy(actor);
        if (!List.of("PARTICIPANTE","RESPONSABLE").contains(function)) throw new IllegalArgumentException("La función no es válida.");
        var locked = db.query("SELECT id FROM usuario WHERE id=? AND academia_id=? AND estado='ACTIVO' FOR UPDATE", (rs,i)->rs.getObject(1,UUID.class), user,academy);
        if (locked.isEmpty()) throw new IllegalArgumentException("Usuario o Tripulación no disponible en tu academia.");
        var crews = db.query("SELECT semillero_id FROM tripulacion WHERE id=? AND academia_id=? AND estado='ACTIVA'",(rs,i)->rs.getObject(1,UUID.class),crew,academy);
        if (crews.isEmpty()) throw new IllegalArgumentException("Usuario o Tripulación no disponible en tu academia.");
        Integer active = db.queryForObject("SELECT COUNT(*) FROM asignacion_semillero a JOIN asignacion_activa x ON x.asignacion_id=a.id WHERE a.usuario_id=? AND a.semillero_id=?",Integer.class,user,crews.getFirst());
        if (active == null || active != 1) throw new IllegalArgumentException("El usuario debe pertenecer al semillero de la Tripulación.");
        var existing = db.query("SELECT id FROM membresia_tripulacion WHERE usuario_id=? AND tripulacion_id=?",(rs,i)->rs.getObject(1,UUID.class),user,crew);
        UUID membership;
        if (existing.isEmpty()) { membership=UUID.randomUUID(); db.update("INSERT INTO membresia_tripulacion(id,academia_id,tripulacion_id,usuario_id) VALUES (?,?,?,?)",membership,academy,crew,user); }
        else membership=existing.getFirst();
        Integer count=db.queryForObject("SELECT COUNT(*) FROM membresia_activa WHERE membresia_id=?",Integer.class,membership);
        if(count != null && count>0) throw new IllegalArgumentException("El usuario ya pertenece a la Tripulación.");
        UUID actorId=db.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor), period=UUID.randomUUID();
        db.update("INSERT INTO periodo_membresia(id,membresia_id,inicio,funcion,registrado_por_id) VALUES (?,?,?,?,?)",period,membership,OffsetDateTime.now(ZoneOffset.UTC),function,actorId);
        db.update("INSERT INTO membresia_activa(membresia_id,periodo_id) VALUES (?,?)",membership,period);
        audit.record(actorId,"INCORPORAR_TRIPULANTE","MEMBRESIA",membership);
    }
    @Transactional
    public void close(String actor, UUID membership) {
        UUID academy=academy(actor);
        var rows=db.query("SELECT m.usuario_id,a.periodo_id FROM membresia_tripulacion m JOIN membresia_activa a ON a.membresia_id=m.id WHERE m.id=? AND m.academia_id=?",(rs,i)->new IdPair(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),membership,academy);
        if(rows.isEmpty()) throw new IllegalArgumentException("Membresía no disponible.");
        var row=rows.getFirst();
        db.query("SELECT id FROM usuario WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),row.user());
        if(db.update("DELETE FROM membresia_activa WHERE membresia_id=? AND periodo_id=?",membership,row.period())!=1) throw new IllegalArgumentException("La membresía ya no está activa.");
        db.update("UPDATE periodo_membresia SET fin=? WHERE id=? AND fin IS NULL",OffsetDateTime.now(ZoneOffset.UTC),row.period());
        audit.record(db.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor),"CERRAR_MEMBRESIA","MEMBRESIA",membership);
    }
    public record Option(UUID id,String name) {}
    public record Crew(UUID id,String name,String track) {}
    public record Member(UUID id,String email,String crew,String function) {}
    public record View(List<Option> tracks,List<Crew> crews,List<Option> users,List<Member> members) {}
    private record IdPair(UUID user,UUID period) {}
}
