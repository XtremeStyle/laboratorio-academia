package com.laboratorio.mission;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operaciones académicas iniciales: crea y asigna Misiones con etapa final. */
@Service
public class MissionService {
    private final JdbcTemplate db;
    private final com.laboratorio.notification.NotificationService notifications;
    private final com.laboratorio.audit.AuditService audit;
    public MissionService(JdbcTemplate db,com.laboratorio.notification.NotificationService notifications,com.laboratorio.audit.AuditService audit) { this.db = db; this.notifications=notifications; this.audit=audit; }
    private Actor actor(String email) {
        var rows=db.query("SELECT u.id,u.academia_id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO' AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo IN ('ADMINISTRADOR','PROFESOR'))",(rs,i)->new Actor(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),email);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return rows.getFirst();
    }
    private boolean authorized(Actor a, UUID track) {
        Integer found=db.queryForObject("""
            SELECT COUNT(*) FROM semillero s WHERE s.id=? AND s.academia_id=? AND s.estado='ACTIVO' AND
              (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR')
               OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=s.id))
            """,Integer.class,track,a.academy(),a.id(),a.id());
        return found != null && found > 0;
    }
    @Transactional(readOnly=true)
    public View view(String email) {
        Actor a=actor(email);
        var tracks=db.query("""
            SELECT s.id,s.nombre FROM semillero s WHERE s.academia_id=? AND s.estado='ACTIVO' AND
            (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR') OR
             EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa aa ON aa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=s.id)) ORDER BY s.nombre LIMIT 100
            """,(rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)),a.academy(),a.id(),a.id());
        var missions=db.query("""
            SELECT m.id,m.titulo,m.modalidad,s.id,s.nombre FROM mision m JOIN mision_semillero ms ON ms.mision_id=m.id JOIN semillero s ON s.id=ms.semillero_id
            WHERE m.academia_id=? AND m.estado='PUBLICADA' AND
            (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR') OR
             EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa aa ON aa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=s.id))
            ORDER BY m.creado_en DESC LIMIT 100
            """,(rs,i)->new Mission(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getObject(4,UUID.class),rs.getString(5)),a.academy(),a.id(),a.id());
        var users=db.query("""
            SELECT DISTINCT u.id,u.correo FROM usuario u JOIN asignacion_semillero x ON x.usuario_id=u.id JOIN asignacion_activa xa ON xa.asignacion_id=x.id
            WHERE u.academia_id=? AND u.estado='ACTIVO' AND (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR') OR EXISTS (SELECT 1 FROM asignacion_semillero own JOIN asignacion_activa oa ON oa.asignacion_id=own.id WHERE own.usuario_id=? AND own.semillero_id=x.semillero_id)) ORDER BY u.correo LIMIT 100
            """,(rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)),a.academy(),a.id(),a.id());
        var crews=db.query("""
            SELECT t.id,t.nombre FROM tripulacion t WHERE t.academia_id=? AND t.estado='ACTIVA' AND (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR') OR EXISTS (SELECT 1 FROM asignacion_semillero own JOIN asignacion_activa oa ON oa.asignacion_id=own.id WHERE own.usuario_id=? AND own.semillero_id=t.semillero_id)) ORDER BY t.nombre LIMIT 100
            """,(rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)),a.academy(),a.id(),a.id());
        return new View(tracks,missions,users,crews);
    }
    @Transactional
    public void create(String email,UUID track,String title,String instructions,String criterion,String modality,String due,List<UUID> additionalTracks) {
        Actor a=actor(email);
        var allTracks=new java.util.LinkedHashSet<UUID>(); allTracks.add(track); if(additionalTracks!=null) allTracks.addAll(additionalTracks);
        if(allTracks.stream().anyMatch(t->!authorized(a,t))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if((modality.equals("INTERDISCIPLINARIA") && allTracks.size()<2) || (!modality.equals("INTERDISCIPLINARIA") && allTracks.size()!=1)) throw new IllegalArgumentException("Una Misión interdisciplinaria necesita al menos dos semilleros; las demás usan uno.");
        title=title.strip(); instructions=instructions.strip(); criterion=criterion.strip();
        if(title.isEmpty() || title.length()>200 || instructions.isEmpty() || instructions.length()>10000 || criterion.isEmpty() || criterion.length()>2000 || !List.of("INDIVIDUAL","GRUPAL","INTERDISCIPLINARIA").contains(modality)) throw new IllegalArgumentException("Completa título, instrucciones, criterio y modalidad válidos.");
        java.time.OffsetDateTime deadline;
        try { deadline=LocalDate.parse(due).atTime(23,59).atZone(ZoneId.of("America/Bogota")).toOffsetDateTime(); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Indica una fecha límite válida."); }
        if(deadline.isBefore(java.time.OffsetDateTime.now())) throw new IllegalArgumentException("La fecha límite no puede estar en el pasado.");
        UUID mission=UUID.randomUUID(), stage=UUID.randomUUID();
        db.update("INSERT INTO mision(id,academia_id,titulo,instrucciones,modalidad,estado,creada_por_id,vencimiento,politica_evaluacion) VALUES (?,?,?,?,?,'PUBLICADA',?,?,'POR_ETAPA')",mission,a.academy(),title,instructions,modality,a.id(),deadline);
        for(UUID selected:allTracks) db.update("INSERT INTO mision_semillero(id,academia_id,mision_id,semillero_id) VALUES (?,?,?,?)",UUID.randomUUID(),a.academy(),mission,selected);
        db.update("INSERT INTO etapa_mision(id,mision_id,titulo,orden,obligatoria,es_final,vencimiento) VALUES (?,?,'Entrega final',1,TRUE,TRUE,?)",stage,mission,deadline);
        db.update("INSERT INTO criterio_evaluacion(id,mision_id,etapa_id,nombre,descripcion,orden) VALUES (?,?,?,'Criterio principal',?,1)",UUID.randomUUID(),mission,stage,criterion);
        audit.record(a.id(),"CREAR_MISION","MISION",mission);
    }
    @Transactional
    public void addStage(String email, UUID mission, String title, String due, boolean required, String criterion, java.math.BigDecimal peso) {
        if(peso != null && peso.signum() < 0) throw new IllegalArgumentException("El peso no puede ser negativo.");
        Actor a=actor(email);
        var rows=db.query("SELECT ms.semillero_id FROM mision m JOIN mision_semillero ms ON ms.mision_id=m.id WHERE m.id=? AND m.academia_id=? AND m.estado='PUBLICADA' FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),mission,a.academy());
        if(rows.isEmpty() || rows.stream().anyMatch(t->!authorized(a,t))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Integer assignments=db.queryForObject("SELECT COUNT(*) FROM asignacion_mision WHERE mision_id=?",Integer.class,mission);
        if(assignments != null && assignments>0) throw new IllegalArgumentException("Las etapas solo pueden añadirse antes de asignar la Misión.");
        title=title.strip(); criterion=criterion.strip();
        if(criterion.isEmpty() || criterion.length()>2000) throw new IllegalArgumentException("Indica un criterio de etapa de hasta 2000 caracteres.");
        if(title.isEmpty() || title.length()>200) throw new IllegalArgumentException("Indica un título de etapa de hasta 200 caracteres.");
        java.time.OffsetDateTime deadline;
        try { deadline=LocalDate.parse(due).atTime(23,59).atZone(ZoneId.of("America/Bogota")).toOffsetDateTime(); }
        catch(RuntimeException ex) { throw new IllegalArgumentException("Indica una fecha de etapa válida."); }
        var finalStage=db.queryForMap("SELECT id,orden,vencimiento FROM etapa_mision WHERE mision_id=? AND es_final=TRUE",mission);
        var finalDeadline=((java.time.OffsetDateTime)finalStage.get("vencimiento"));
        if(deadline.isBefore(java.time.OffsetDateTime.now()) || deadline.isAfter(finalDeadline)) throw new IllegalArgumentException("La fecha debe ser futura y no superar la fecha final de la Misión.");
        int order=((Number)finalStage.get("orden")).intValue();
        db.update("UPDATE etapa_mision SET orden=? WHERE id=?",order+1,finalStage.get("id"));
        UUID stage=UUID.randomUUID();
        db.update("INSERT INTO etapa_mision(id,mision_id,titulo,orden,obligatoria,es_final,vencimiento) VALUES (?,?,?,?,?,FALSE,?)",stage,mission,title,order,required,deadline);
        peso=peso==null ? java.math.BigDecimal.ZERO : peso;
        db.update("INSERT INTO criterio_evaluacion(id,mision_id,etapa_id,nombre,descripcion,orden,peso) VALUES (?,?,?,'Criterio principal',?,1,?)",UUID.randomUUID(),mission,stage,criterion,peso);
        audit.record(a.id(),"CREAR_ETAPA","ETAPA",stage);
    }
    @Transactional
    public void assign(String email,UUID mission,UUID target,UUID selectedTrack,String requestedType) {
        Actor a=actor(email);
        // La asignación y la edición de etapas se serializan sobre la misma Misión.
        db.query("SELECT id FROM mision WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),mission);
        var rows=db.query("SELECT m.modalidad,ms.semillero_id FROM mision m JOIN mision_semillero ms ON ms.mision_id=m.id WHERE m.id=? AND m.academia_id=? AND m.estado='PUBLICADA'",(rs,i)->new MTarget(rs.getString(1),rs.getObject(2,UUID.class)),mission,a.academy());
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(selectedTrack==null && rows.size()==1) selectedTrack=rows.getFirst().track();
        final UUID chosen=selectedTrack;
        var selected=rows.stream().filter(r->r.track().equals(chosen)).findFirst();
        if(selected.isEmpty() || !authorized(a,chosen)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var m=selected.get(); UUID assignment=UUID.randomUUID();
        String type=m.mode().equals("INTERDISCIPLINARIA") ? requestedType : m.mode();
        if(type.equals("INDIVIDUAL")) {
            Integer n=db.queryForObject("SELECT COUNT(*) FROM usuario u JOIN asignacion_semillero x ON x.usuario_id=u.id JOIN asignacion_activa aa ON aa.asignacion_id=x.id WHERE u.id=? AND u.academia_id=? AND u.estado='ACTIVO' AND x.semillero_id=? AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE')",Integer.class,target,a.academy(),m.track());
            if(n==null || n!=1) throw new IllegalArgumentException("El destinatario debe ser estudiante y tener una asignación activa al semillero.");
            db.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,destinatario_usuario_id,semillero_destino_id,estado) VALUES (?,?,?,?,?,'ACTIVA')",assignment,a.academy(),mission,target,chosen);
            db.update("INSERT INTO participante_asignacion(id,asignacion_id,usuario_id,incorporado_en) VALUES (?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),assignment,target);
        } else if(type.equals("GRUPAL")) {
            Integer n=db.queryForObject("SELECT COUNT(*) FROM tripulacion WHERE id=? AND academia_id=? AND semillero_id=? AND estado='ACTIVA'",Integer.class,target,a.academy(),m.track());
            if(n==null || n!=1) throw new IllegalArgumentException("La Tripulación no pertenece al semillero de la Misión.");
            var members=db.query("SELECT m.usuario_id FROM membresia_tripulacion m JOIN membresia_activa ma ON ma.membresia_id=m.id JOIN usuario u ON u.id=m.usuario_id WHERE m.tripulacion_id=? AND u.estado='ACTIVO'",(rs,i)->rs.getObject(1,UUID.class),target);
            if(members.isEmpty()) throw new IllegalArgumentException("La Tripulación necesita integrantes activos.");
            db.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,destinatario_tripulacion_id,semillero_destino_id,estado) VALUES (?,?,?,?,?,'ACTIVA')",assignment,a.academy(),mission,target,chosen);
            for(UUID member:members) db.update("INSERT INTO participante_asignacion(id,asignacion_id,usuario_id,incorporado_en) VALUES (?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),assignment,member);
        } else throw new IllegalArgumentException("Esta modalidad aún no admite asignaciones.");
        audit.record(a.id(),"ASIGNAR_MISION","ASIGNACION",assignment);
        notifications.notifyAssignment(assignment,"MISION_ASIGNADA","Tienes una nueva Misión asignada. Consulta Mis Misiones para ver los detalles.");
    }
    @Transactional(readOnly=true)
    public List<CompletionOption> completionOptions(String email) {
        Actor a=actor(email);
        return db.query("""
          SELECT am.id,m.titulo,s.nombre,COALESCE(u.correo,t.nombre) FROM asignacion_mision am
          JOIN mision m ON m.id=am.mision_id JOIN semillero s ON s.id=am.semillero_destino_id
          LEFT JOIN usuario u ON u.id=am.destinatario_usuario_id LEFT JOIN tripulacion t ON t.id=am.destinatario_tripulacion_id
          WHERE am.academia_id=? AND am.estado='ACTIVA' AND m.estado='PUBLICADA' AND
            (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR')
              OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa aa ON aa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=s.id))
          ORDER BY m.titulo,s.nombre LIMIT 100
          """,(rs,i)->new CompletionOption(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4)),a.academy(),a.id(),a.id());
    }
    @Transactional
    public void complete(String email,UUID assignment) {
        Actor a=actor(email);
        var rows=db.query("SELECT am.mision_id,am.semillero_destino_id FROM asignacion_mision am JOIN mision m ON m.id=am.mision_id WHERE am.id=? AND am.academia_id=? AND am.estado='ACTIVA' AND m.estado='PUBLICADA' FOR UPDATE",(rs,i)->new MTarget("",rs.getObject(2,UUID.class)),assignment,a.academy());
        if(rows.size()!=1 || !authorized(a,rows.getFirst().track())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Integer ready=db.queryForObject("""
          SELECT COUNT(*) FROM asignacion_mision am JOIN etapa_mision st ON st.mision_id=am.mision_id AND st.es_final=TRUE
          JOIN entrega_etapa d ON d.asignacion_id=am.id AND d.etapa_id=st.id JOIN evaluacion e ON e.entrega_etapa_id=d.id
          JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
          JOIN version_entrega v ON v.id=ev.version_entrega_id AND v.numero=(SELECT MAX(v2.numero) FROM version_entrega v2 WHERE v2.entrega_etapa_id=d.id)
          WHERE am.id=? AND NOT EXISTS (SELECT 1 FROM etapa_mision x WHERE x.mision_id=am.mision_id AND x.obligatoria=TRUE AND NOT EXISTS
            (SELECT 1 FROM entrega_etapa dx JOIN version_entrega vx ON vx.entrega_etapa_id=dx.id WHERE dx.asignacion_id=am.id AND dx.etapa_id=x.id))
          """,Integer.class,assignment);
        if(ready==null || ready!=1) throw new IllegalArgumentException("Antes de cerrar, deben entregarse las etapas obligatorias y evaluarse la versión final más reciente.");
        db.update("UPDATE asignacion_mision SET estado='COMPLETADA' WHERE id=?",assignment);
        audit.record(a.id(),"COMPLETAR_MISION","ASIGNACION",assignment);
        notifications.notifyAssignment(assignment,"MISION_COMPLETADA","Se cerró una de tus Misiones. Puedes consultar su historial y evaluaciones.");
    }
    public record CompletionOption(UUID id,String mission,String track,String recipient) {}
    @Transactional(readOnly=true)
    public MissionPage minePage(String email,int page) {
        if(page<0 || page>20000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Página no válida.");
        var rows=mine(email,true,51,page*50);
        boolean more=rows.size()>50;
        return new MissionPage(List.copyOf(rows.subList(0,Math.min(rows.size(),50))),more,page);
    }
    @Transactional(readOnly=true)
    public List<MyMission> mineForReport(String email) { return mine(email,true,0,0); }
    private List<MyMission> mine(String email,boolean includeCompleted,int limit,int offset) {
        return db.query("""
            SELECT DISTINCT m.titulo,m.instrucciones,m.modalidad,m.vencimiento,am.id,
            (SELECT AVG(ev.calificacion_base+COALESCE(ai.ajuste,0)) FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
             JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
             LEFT JOIN ajuste_individual ai ON ai.version_evaluacion_id=ev.id AND ai.usuario_id=pa.usuario_id
             WHERE d.asignacion_id=am.id AND ev.calificacion_base+COALESCE(ai.ajuste,0) BETWEEN 0 AND 5),
            (SELECT COUNT(*) FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
             JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
             WHERE d.asignacion_id=am.id AND (ev.calificacion_base<0 OR ev.calificacion_base>5)),am.estado FROM mision m
            JOIN asignacion_mision am ON am.mision_id=m.id JOIN participante_asignacion pa ON pa.asignacion_id=am.id
            JOIN usuario u ON u.id=pa.usuario_id WHERE u.correo=? AND u.estado='ACTIVO' AND pa.retirado_en IS NULL AND (am.estado='ACTIVA' OR (? AND am.estado='COMPLETADA'))
            ORDER BY m.vencimiento,m.titulo,am.id
            """+(limit>0 ? " LIMIT "+limit+" OFFSET "+offset : ""),(rs,i)->new MyMission(rs.getString(1),rs.getString(2),rs.getString(3),rs.getObject(4,java.time.OffsetDateTime.class),rs.getObject(5,UUID.class),rs.getBigDecimal(6),rs.getInt(7),rs.getString(8)),email,includeCompleted);
    }
    private record Actor(UUID id,UUID academy) {}
    private record MTarget(String mode,UUID track) {}
    public record Option(UUID id,String name) {}
    public record Mission(UUID id,String title,String mode,UUID track,String trackName) {}
    public record View(List<Option> tracks,List<Mission> missions,List<Option> users,List<Option> crews) {}
    public record MissionPage(List<MyMission> items,boolean more,int number) {}
    public record MyMission(String title,String instructions,String mode,java.time.OffsetDateTime deadline,UUID assignment,java.math.BigDecimal average,int invalidGrades,String state) {}
}
