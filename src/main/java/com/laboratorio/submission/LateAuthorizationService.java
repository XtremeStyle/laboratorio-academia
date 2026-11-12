package com.laboratorio.submission;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LateAuthorizationService {
    private final JdbcTemplate db;
    private final com.laboratorio.audit.AuditService audit;
    private final com.laboratorio.notification.NotificationService notifications;
    public LateAuthorizationService(JdbcTemplate db,com.laboratorio.audit.AuditService audit,com.laboratorio.notification.NotificationService notifications) { this.db=db; this.audit=audit; this.notifications=notifications; }
    private UUID actor(String email) {
        var ids=db.query("SELECT u.id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO' AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo IN ('PROFESOR','ADMINISTRADOR'))",(rs,i)->rs.getObject(1,UUID.class),email);
        if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ids.getFirst();
    }
    @Transactional(readOnly=true)
    public List<Option> options(String email) {
        UUID teacher=actor(email);
        return db.query("""
          SELECT DISTINCT am.id,m.titulo,COALESCE(u.correo,t.nombre),e.id,e.titulo FROM asignacion_mision am JOIN mision m ON m.id=am.mision_id JOIN etapa_mision e ON e.mision_id=m.id
          JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id LEFT JOIN usuario u ON u.id=am.destinatario_usuario_id
          LEFT JOIN tripulacion t ON t.id=am.destinatario_tripulacion_id
          WHERE am.estado='ACTIVA' AND m.estado='PUBLICADA' AND
           (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id JOIN usuario x ON x.id=ur.usuario_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR' AND x.academia_id=m.academia_id)
            OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa aa ON aa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=ms.semillero_id))
          ORDER BY m.titulo LIMIT 100
          """,(rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getObject(4,UUID.class),rs.getString(5)),teacher,teacher);
    }
    @Transactional
    public void authorize(String email,UUID assignment,String date,String reason) { authorize(email,assignment,null,date,reason); }
    @Transactional
    public void authorize(String email,UUID assignment,UUID stage,String date,String reason) {
        UUID teacher=actor(email);
        reason=reason.strip();
        if(reason.isEmpty() || reason.length()>2000) throw new IllegalArgumentException("Indica un motivo de hasta 2000 caracteres.");
        OffsetDateTime until;
        try { until=LocalDate.parse(date).atTime(23,59).atZone(ZoneId.of("America/Bogota")).toOffsetDateTime(); }
        catch(RuntimeException ex) { throw new IllegalArgumentException("Indica una fecha de autorización válida."); }
        if(!until.isAfter(OffsetDateTime.now())) throw new IllegalArgumentException("La autorización debe vencer en el futuro.");
        db.query("SELECT id FROM asignacion_mision WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),assignment);
        var rows=db.query("""
          SELECT m.id,e.id FROM asignacion_mision am JOIN mision m ON m.id=am.mision_id JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
          JOIN etapa_mision e ON e.mision_id=m.id AND ((CAST(? AS UUID) IS NULL AND e.es_final=TRUE) OR e.id=?) WHERE am.id=? AND am.estado='ACTIVA' AND m.estado='PUBLICADA' AND
          (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id JOIN usuario x ON x.id=ur.usuario_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR' AND x.academia_id=m.academia_id)
          OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=ms.semillero_id))
          """,(rs,i)->new Pair(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),stage,stage,assignment,teacher,teacher);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var pair=rows.getFirst();
        var submissions=db.query("SELECT id FROM entrega_etapa WHERE asignacion_id=? AND etapa_id=?",(rs,i)->rs.getObject(1,UUID.class),assignment,pair.stage());
        UUID delivery;
        if(submissions.isEmpty()) { delivery=UUID.randomUUID(); db.update("INSERT INTO entrega_etapa(id,mision_id,asignacion_id,etapa_id) VALUES (?,?,?,?)",delivery,pair.mission(),assignment,pair.stage()); }
        else delivery=submissions.getFirst();
        UUID authorization=UUID.randomUUID();
        db.update("INSERT INTO autorizacion_tardia(id,entrega_etapa_id,concedida_por_id,motivo,concedida_en,valida_hasta) VALUES (?,?,?,?,CURRENT_TIMESTAMP,?)",authorization,delivery,teacher,reason,until);
        audit.record(teacher,"AUTORIZAR_ENTREGA_TARDIA","AUTORIZACION",authorization);
        notifications.notifyAssignment(assignment,"AUTORIZACION_TARDIA","Se autorizó una entrega tardía en una de tus Misiones. Consulta con tu docente los detalles.");
    }
    @Transactional(readOnly=true)
    public List<Permission> permissions(String email) {
        UUID teacher=actor(email);
        return db.query("""
          SELECT DISTINCT at.id,m.titulo,et.titulo,COALESCE(u.correo,t.nombre),at.motivo,at.valida_hasta
          FROM autorizacion_tardia at JOIN entrega_etapa d ON d.id=at.entrega_etapa_id
          JOIN etapa_mision et ON et.id=d.etapa_id JOIN asignacion_mision am ON am.id=d.asignacion_id
          JOIN mision m ON m.id=d.mision_id JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
          LEFT JOIN usuario u ON u.id=am.destinatario_usuario_id LEFT JOIN tripulacion t ON t.id=am.destinatario_tripulacion_id
          WHERE am.estado='ACTIVA' AND at.revocada_en IS NULL AND (at.valida_hasta IS NULL OR at.valida_hasta>=CURRENT_TIMESTAMP) AND
          (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id JOIN usuario x ON x.id=ur.usuario_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR' AND x.academia_id=m.academia_id)
           OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=ms.semillero_id))
          ORDER BY m.titulo LIMIT 100
          """,(rs,i)->new Permission(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getObject(6,OffsetDateTime.class)),teacher,teacher);
    }
    @Transactional
    public void revoke(String email,UUID permission) {
        UUID teacher=actor(email);
        var assignments=db.query("SELECT d.asignacion_id FROM autorizacion_tardia at JOIN entrega_etapa d ON d.id=at.entrega_etapa_id WHERE at.id=?",(rs,i)->rs.getObject(1,UUID.class),permission);
        if(assignments.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        db.query("SELECT id FROM asignacion_mision WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),assignments.getFirst());
        Integer permitted=db.queryForObject("""
          SELECT COUNT(*) FROM autorizacion_tardia at JOIN entrega_etapa d ON d.id=at.entrega_etapa_id
          JOIN asignacion_mision am ON am.id=d.asignacion_id JOIN mision m ON m.id=d.mision_id JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
          WHERE at.id=? AND (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id JOIN usuario x ON x.id=ur.usuario_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR' AND x.academia_id=m.academia_id)
            OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=ms.semillero_id))
          """,Integer.class,permission,teacher,teacher);
        if(permitted==null || permitted==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        int changed=db.update("UPDATE autorizacion_tardia SET revocada_en=CURRENT_TIMESTAMP WHERE id=? AND revocada_en IS NULL",permission);
        if(changed==0) throw new IllegalArgumentException("La autorización ya fue revocada.");
        audit.record(teacher,"REVOCAR_AUTORIZACION","AUTORIZACION",permission);
        notifications.notifyAssignment(assignments.getFirst(),"AUTORIZACION_REVOCADA","Se revocó una autorización de entrega tardía en una de tus Misiones. Consulta con tu docente los detalles.");
    }
    public record Permission(UUID id,String mission,String stage,String recipient,String reason,OffsetDateTime until) {}
    public record Option(UUID id,String title,String recipient,UUID stage,String stageTitle) {}
    private record Pair(UUID mission,UUID stage) {}
}
