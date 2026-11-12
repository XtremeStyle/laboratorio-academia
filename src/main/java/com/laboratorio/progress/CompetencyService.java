package com.laboratorio.progress;

import com.laboratorio.audit.AuditService;
import com.laboratorio.mission.MissionService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompetencyService {
    private final JdbcTemplate db;
    private final MissionService missions;
    private final AuditService audit;
    public CompetencyService(JdbcTemplate db,MissionService missions,AuditService audit) { this.db=db; this.missions=missions; this.audit=audit; }
    private Actor actor(String email) {
        var rows=db.query("SELECT id,academia_id FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->new Actor(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),email);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Set<UUID> tracks=missions.view(email).tracks().stream().map(MissionService.Option::id).collect(Collectors.toSet());
        return new Actor(rows.getFirst().id(),rows.getFirst().academy(),tracks);
    }
    @Transactional(readOnly=true)
    public View view(String email) {
        Actor a=actor(email);
        var tracks=missions.view(email).tracks();
        var competencies=db.query("SELECT c.id,c.nombre,s.id,s.nombre FROM competencia c JOIN competencia_semillero cs ON cs.competencia_id=c.id JOIN semillero s ON s.id=cs.semillero_id WHERE c.academia_id=? AND c.estado='ACTIVA' ORDER BY c.nombre,s.nombre",(rs,i)->new Choice(rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,UUID.class),rs.getString(4)),a.academy()).stream().filter(c->a.tracks().contains(c.track())).toList();
        var criteria=db.query("""
          SELECT c.id,m.titulo || ' · ' || COALESCE(e.titulo,'General') || ' · ' || c.descripcion,ms.semillero_id,s.nombre
          FROM criterio_evaluacion c JOIN mision m ON m.id=c.mision_id JOIN mision_semillero ms ON ms.mision_id=m.id
          JOIN semillero s ON s.id=ms.semillero_id LEFT JOIN etapa_mision e ON e.id=c.etapa_id
          WHERE m.academia_id=? AND m.estado='PUBLICADA' AND NOT EXISTS (SELECT 1 FROM asignacion_mision am WHERE am.mision_id=m.id)
          ORDER BY m.titulo,c.orden
          """,(rs,i)->new Choice(rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,UUID.class),rs.getString(4)),a.academy()).stream().filter(c->a.tracks().contains(c.track())).toList();
        return new View(tracks,competencies,criteria);
    }
    @Transactional
    public void create(String email,UUID track,String code,String name) {
        Actor a=actor(email);
        if(!a.tracks().contains(track)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        code=code.strip().toUpperCase(Locale.ROOT); name=name.strip();
        if(!code.matches("[A-Z0-9_-]{1,50}") || name.isEmpty() || name.length()>160) throw new IllegalArgumentException("Indica un código válido y un nombre de hasta 160 caracteres.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO competencia(id,academia_id,codigo,nombre,estado) VALUES (?,?,?,?,'ACTIVA')",id,a.academy(),code,name);
        db.update("INSERT INTO competencia_semillero(id,academia_id,competencia_id,semillero_id) VALUES (?,?,?,?)",UUID.randomUUID(),a.academy(),id,track);
        audit.record(a.id(),"CREAR_COMPETENCIA","COMPETENCIA",id);
    }
    @Transactional
    public void link(String email,UUID criterion,UUID competency) {
        Actor a=actor(email);
        var ids=db.query("SELECT m.id FROM criterio_evaluacion c JOIN mision m ON m.id=c.mision_id WHERE c.id=? AND m.academia_id=?",(rs,i)->rs.getObject(1,UUID.class),criterion,a.academy());
        if(ids.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        UUID mission=ids.getFirst();
        db.query("SELECT id FROM mision WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),mission);
        var tracks=db.query("SELECT ms.semillero_id FROM mision_semillero ms JOIN competencia_semillero cs ON cs.semillero_id=ms.semillero_id WHERE ms.mision_id=? AND cs.competencia_id=? AND cs.academia_id=?",(rs,i)->rs.getObject(1,UUID.class),mission,competency,a.academy());
        if(tracks.size()!=1 || !a.tracks().contains(tracks.getFirst())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Integer count=db.queryForObject("SELECT COUNT(*) FROM asignacion_mision WHERE mision_id=?",Integer.class,mission);
        if(count!=null && count>0) throw new IllegalArgumentException("Vincula competencias antes de asignar la Misión.");
        db.update("INSERT INTO criterio_competencia(id,criterio_id,competencia_id) VALUES (?,?,?)",UUID.randomUUID(),criterion,competency);
        audit.record(a.id(),"VINCULAR_COMPETENCIA","CRITERIO",criterion);
    }
    private record Actor(UUID id,UUID academy,Set<UUID> tracks) { Actor(UUID id,UUID academy) { this(id,academy,Set.of()); } }
    public record Choice(UUID id,String name,UUID track,String trackName) {}
    public record View(List<MissionService.Option> tracks,List<Choice> competencies,List<Choice> criteria) {}
}
