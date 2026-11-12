package com.laboratorio.evaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvaluationService {
    private final JdbcTemplate db;
    private final com.laboratorio.notification.NotificationService notifications;
    private final com.laboratorio.audit.AuditService audit;
    private final com.laboratorio.gamification.BadgeService badges;
    private final com.laboratorio.progress.PerformanceAlertService alerts;
    private final com.laboratorio.gamification.XpMovementService xp;
    public EvaluationService(JdbcTemplate db,com.laboratorio.notification.NotificationService notifications,com.laboratorio.audit.AuditService audit,com.laboratorio.gamification.BadgeService badges,com.laboratorio.progress.PerformanceAlertService alerts,com.laboratorio.gamification.XpMovementService xp) { this.db=db; this.notifications=notifications; this.audit=audit; this.badges=badges; this.alerts=alerts; this.xp=xp; }
    private UUID actor(String email) {
        var ids=db.query("SELECT u.id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO' AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo IN ('PROFESOR','ADMINISTRADOR'))",(rs,i)->rs.getObject(1,UUID.class),email);
        if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ids.getFirst();
    }
    @Transactional(readOnly=true)
    public List<Delivery> deliveries(String email) {
        UUID actor=actor(email);
        var deliveries=db.query("""
          SELECT e.id,m.titulo,v.numero,v.contenido,COALESCE(u.correo,t.nombre),v.enviada_en,et.titulo,at.id,at.nombre_original
          FROM entrega_etapa e JOIN etapa_mision et ON et.id=e.etapa_id JOIN asignacion_mision am ON am.id=e.asignacion_id
          JOIN mision m ON m.id=e.mision_id JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
          JOIN version_entrega v ON v.entrega_etapa_id=e.id AND v.numero=(SELECT MAX(v2.numero) FROM version_entrega v2 WHERE v2.entrega_etapa_id=e.id)
          LEFT JOIN adjunto_entrega at ON at.version_id=v.id LEFT JOIN usuario u ON u.id=am.destinatario_usuario_id LEFT JOIN tripulacion t ON t.id=am.destinatario_tripulacion_id
          WHERE (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id JOIN usuario x ON x.id=ur.usuario_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR' AND x.academia_id=m.academia_id)
             OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=ms.semillero_id))
          ORDER BY v.enviada_en DESC LIMIT 100
          """,(rs,i)->new Delivery(rs.getObject(1,UUID.class),rs.getString(2),rs.getInt(3),rs.getString(4),rs.getString(5),rs.getObject(6,java.time.OffsetDateTime.class),rs.getString(7),rs.getObject(8,UUID.class),rs.getString(9),java.util.List.of(),java.util.List.of()),actor,actor);
        if(deliveries.isEmpty()) return deliveries;
        String placeholders=String.join(",",java.util.Collections.nCopies(deliveries.size(),"?"));
        var rows=db.query("SELECT d.id,c.id,c.descripcion,CASE WHEN EXISTS (SELECT 1 FROM criterio_competencia cc JOIN competencia_semillero cs ON cs.competencia_id=cc.competencia_id WHERE cc.criterio_id=c.id AND cs.semillero_id=am.semillero_destino_id) THEN TRUE ELSE FALSE END FROM entrega_etapa d JOIN asignacion_mision am ON am.id=d.asignacion_id JOIN criterio_evaluacion c ON c.mision_id=d.mision_id AND (c.etapa_id IS NULL OR c.etapa_id=d.etapa_id) WHERE d.id IN ("+placeholders+") ORDER BY c.orden,c.id",
            (rs,i)->new CriterionRow(rs.getObject(1,UUID.class),new Criterion(rs.getObject(2,UUID.class),rs.getString(3),rs.getBoolean(4))),deliveries.stream().map(Delivery::id).toArray());
        var byDelivery=rows.stream().collect(java.util.stream.Collectors.groupingBy(CriterionRow::delivery,java.util.stream.Collectors.mapping(CriterionRow::criterion,java.util.stream.Collectors.toList())));
        var participants=db.query("""
            SELECT d.id,u.id,u.nombre,u.apellido,ai.ajuste,ai.justificacion FROM entrega_etapa d
            JOIN asignacion_mision am ON am.id=d.asignacion_id AND am.destinatario_tripulacion_id IS NOT NULL
            JOIN participante_asignacion pa ON pa.asignacion_id=am.id AND pa.retirado_en IS NULL JOIN usuario u ON u.id=pa.usuario_id
            LEFT JOIN evaluacion e ON e.entrega_etapa_id=d.id LEFT JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
            LEFT JOIN ajuste_individual ai ON ai.version_evaluacion_id=ev.id AND ai.usuario_id=u.id WHERE d.id IN ("""+placeholders+") ORDER BY u.apellido,u.nombre",(rs,i)->new MemberRow(rs.getObject(1,UUID.class),new Member(rs.getObject(2,UUID.class),rs.getString(3)+" "+rs.getString(4),rs.getBigDecimal(5),rs.getString(6))),deliveries.stream().map(Delivery::id).toArray());
        var members=participants.stream().collect(java.util.stream.Collectors.groupingBy(MemberRow::delivery,java.util.stream.Collectors.mapping(MemberRow::member,java.util.stream.Collectors.toList())));
        return deliveries.stream().map(d->new Delivery(d.id(),d.title(),d.version(),d.content(),d.recipient(),d.submitted(),d.stage(),d.attachment(),d.attachmentName(),byDelivery.getOrDefault(d.id(),java.util.List.of()),members.getOrDefault(d.id(),java.util.List.of()))).toList();
    }
    @Transactional
    public void grade(String email, UUID delivery, String scoreText, String comment, java.util.Map<String,String> parameters) {
        UUID teacher=actor(email);
        BigDecimal score;
        try { score=new BigDecimal(scoreText.strip()); } catch(NumberFormatException ex) { throw new IllegalArgumentException("Introduce una calificación numérica válida."); }
        if(score.scale()>4 || score.signum()<0 || score.compareTo(new BigDecimal("5"))>0 || comment.length()>2000) throw new IllegalArgumentException("La calificación debe estar entre 0 y 5, con hasta cuatro decimales, y la observación de hasta 2000 caracteres.");
        var assignments=db.query("SELECT asignacion_id FROM entrega_etapa WHERE id=?",(rs,i)->rs.getObject(1,UUID.class),delivery);
        if(assignments.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // Orden común con el envío: primero asignación, después entrega.
        db.query("SELECT id FROM asignacion_mision WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),assignments.getFirst());
        db.query("SELECT id FROM entrega_etapa WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),delivery);
        Integer permitted=db.queryForObject("""
          SELECT COUNT(*) FROM entrega_etapa e JOIN asignacion_mision am ON am.id=e.asignacion_id JOIN mision m ON m.id=e.mision_id JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
          WHERE e.id=? AND (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id JOIN usuario x ON x.id=ur.usuario_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR' AND x.academia_id=m.academia_id)
          OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=ms.semillero_id))
          """,Integer.class,delivery,teacher,teacher);
        if(permitted==null || permitted==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var criteria=db.query("SELECT c.id,CASE WHEN EXISTS (SELECT 1 FROM criterio_competencia cc JOIN competencia_semillero cs ON cs.competencia_id=cc.competencia_id WHERE cc.criterio_id=c.id AND cs.semillero_id=am.semillero_destino_id) THEN TRUE ELSE FALSE END FROM entrega_etapa d JOIN asignacion_mision am ON am.id=d.asignacion_id JOIN criterio_evaluacion c ON c.mision_id=d.mision_id AND (c.etapa_id IS NULL OR c.etapa_id=d.etapa_id) WHERE d.id=?",(rs,i)->new Criterion(rs.getObject(1,UUID.class),"",rs.getBoolean(2)),delivery);
        java.util.Map<UUID,BigDecimal> scores=new java.util.HashMap<>();
        for(Criterion criterion:criteria) {
            String value=parameters.getOrDefault("criterio_"+criterion.id(),"").strip();
            if(value.isEmpty()) { if(criterion.required()) throw new IllegalArgumentException("Califica cada criterio vinculado a una competencia."); continue; }
            BigDecimal criterionScore;
            try { criterionScore=new BigDecimal(value); } catch(NumberFormatException ex) { throw new IllegalArgumentException("La nota de cada criterio debe ser numérica."); }
            if(criterionScore.signum()<0 || criterionScore.compareTo(new BigDecimal("5"))>0 || criterionScore.scale()>4) throw new IllegalArgumentException("Las notas de los criterios deben estar entre 0 y 5, con hasta cuatro decimales.");
            scores.put(criterion.id(),criterionScore);
        }
        var versions=db.query("SELECT id FROM version_entrega WHERE entrega_etapa_id=? ORDER BY numero DESC LIMIT 1",(rs,i)->rs.getObject(1,UUID.class),delivery);
        if(versions.isEmpty()) throw new IllegalArgumentException("No hay una entrega que evaluar.");
        var evaluations=db.query("SELECT id FROM evaluacion WHERE entrega_etapa_id=?",(rs,i)->rs.getObject(1,UUID.class),delivery);
        UUID evaluation;
        if(evaluations.isEmpty()) { evaluation=UUID.randomUUID(); db.update("INSERT INTO evaluacion(id,entrega_etapa_id) VALUES (?,?)",evaluation,delivery); }
        else evaluation=evaluations.getFirst();
        var members=db.query("SELECT pa.usuario_id FROM participante_asignacion pa JOIN asignacion_mision am ON am.id=pa.asignacion_id WHERE pa.asignacion_id=? AND pa.retirado_en IS NULL AND am.destinatario_tripulacion_id IS NOT NULL",(rs,i)->rs.getObject(1,UUID.class),assignments.getFirst());
        var previous=db.query("SELECT ai.usuario_id,ai.ajuste,ai.justificacion FROM ajuste_individual ai JOIN version_evaluacion ev ON ev.id=ai.version_evaluacion_id WHERE ev.evaluacion_id=? AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=ev.evaluacion_id)",(rs,i)->new Adjustment(rs.getObject(1,UUID.class),rs.getBigDecimal(2),rs.getString(3)),evaluation);
        java.util.Map<UUID,Adjustment> adjustments=new java.util.HashMap<>();
        for(Adjustment item:previous) if(members.contains(item.user())) adjustments.put(item.user(),item);
        for(UUID member:members) {
            String key="ajuste_"+member, reasonKey="justificacion_"+member;
            if(!parameters.containsKey(key) && !parameters.containsKey(reasonKey)) continue;
            String value=parameters.getOrDefault(key,"").strip(),reason=parameters.getOrDefault(reasonKey,"").strip();
            if(value.isEmpty() && reason.isEmpty()) { adjustments.remove(member); continue; }
            BigDecimal delta;
            try { delta=new BigDecimal(value); } catch(NumberFormatException ex) { throw new IllegalArgumentException("Indica un ajuste numérico y su justificación."); }
            if(reason.isEmpty() || reason.length()>2000 || delta.scale()>4) throw new IllegalArgumentException("Cada ajuste requiere una justificación de hasta 2000 caracteres y hasta cuatro decimales.");
            adjustments.put(member,new Adjustment(member,delta,reason));
        }
        for(Adjustment item:adjustments.values()) if(score.add(item.delta()).signum()<0 || score.add(item.delta()).compareTo(new BigDecimal("5"))>0) throw new IllegalArgumentException("La nota ajustada de cada integrante debe estar entre 0 y 5.");
        Integer number=db.queryForObject("SELECT COALESCE(MAX(numero),0)+1 FROM version_evaluacion WHERE evaluacion_id=?",Integer.class,evaluation);
        UUID evaluationVersion=UUID.randomUUID();
        db.update("INSERT INTO version_evaluacion(id,evaluacion_id,entrega_etapa_id,numero,version_entrega_id,evaluador_id,calificacion_base,observacion,registrada_en) VALUES (?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",evaluationVersion,evaluation,delivery,number,versions.getFirst(),teacher,score,comment.strip());
        for(var entry:scores.entrySet()) db.update("INSERT INTO puntaje_criterio(id,version_evaluacion_id,criterio_id,puntaje) VALUES (?,?,?,?)",UUID.randomUUID(),evaluationVersion,entry.getKey(),entry.getValue());
        for(Adjustment item:adjustments.values()) db.update("INSERT INTO ajuste_individual(id,version_evaluacion_id,usuario_id,ajuste,justificacion) VALUES (?,?,?,?,?)",UUID.randomUUID(),evaluationVersion,item.user(),item.delta(),item.reason());
        badges.reconcile(assignments.getFirst());
        alerts.reconcile(assignments.getFirst());
        xp.recalcForAssignment(assignments.getFirst(), evaluation);
        audit.record(teacher,"REGISTRAR_EVALUACION","EVALUACION",evaluation);
        notifications.notifyAssignment(assignments.getFirst(),"EVALUACION_REGISTRADA","Se registró una evaluación o corrección en una de tus Misiones. Consulta tus entregas.");
    }
    private record CriterionRow(UUID delivery,Criterion criterion) {}
    private record MemberRow(UUID delivery,Member member) {}
    private record Adjustment(UUID user,BigDecimal delta,String reason) {}
    public record Member(UUID id,String name,BigDecimal delta,String reason) {}
    public record Criterion(UUID id,String description,boolean required) {}
    public record Delivery(UUID id,String title,int version,String content,String recipient,java.time.OffsetDateTime submitted,String stage,UUID attachment,String attachmentName,List<Criterion> criteria,List<Member> members) {}
}
