package com.laboratorio.submission;

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
public class SubmissionService {
    private final JdbcTemplate db;
    private final com.laboratorio.attachment.AttachmentService attachments;
    private final com.laboratorio.audit.AuditService audit;
    public SubmissionService(JdbcTemplate db,com.laboratorio.attachment.AttachmentService attachments,com.laboratorio.audit.AuditService audit) { this.db=db; this.attachments=attachments; this.audit=audit; }
    private Access access(String email, UUID assignment, UUID stage) {
        var rows=db.query("""
            SELECT am.mision_id,u.id,m.titulo,e.id,e.vencimiento,e.es_final,am.estado,m.estado FROM asignacion_mision am
            JOIN mision m ON m.id=am.mision_id JOIN etapa_mision e ON e.mision_id=m.id AND ((CAST(? AS UUID) IS NULL AND e.es_final=TRUE) OR e.id=?)
            JOIN participante_asignacion p ON p.asignacion_id=am.id JOIN usuario u ON u.id=p.usuario_id
            WHERE am.id=? AND am.estado IN ('ACTIVA','COMPLETADA') AND m.estado IN ('PUBLICADA','CERRADA') AND u.correo=? AND u.estado='ACTIVO' AND p.retirado_en IS NULL
            """,(rs,i)->new Access(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getObject(4,UUID.class),rs.getObject(5,OffsetDateTime.class),rs.getBoolean(6),rs.getString(7).equals("ACTIVA") && rs.getString(8).equals("PUBLICADA")),stage,stage,assignment,email);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return rows.getFirst();
    }
    @Transactional(readOnly=true)
    public View view(String email, UUID assignment) { return view(email,assignment,null); }
    @Transactional(readOnly=true)
    public View view(String email, UUID assignment, UUID stage) {
        Access a=access(email,assignment,stage);
        var stages=db.query("SELECT id,titulo,obligatoria FROM etapa_mision WHERE mision_id=? ORDER BY orden",(rs,i)->new Stage(rs.getObject(1,UUID.class),rs.getString(2),rs.getBoolean(3)),a.mission());
        var versions=db.query("""
            SELECT v.numero,v.contenido,v.enviada_en,u.nombre,u.apellido,a.id,a.nombre_original FROM version_entrega v
            JOIN entrega_etapa e ON e.id=v.entrega_etapa_id JOIN usuario u ON u.id=v.autor_id LEFT JOIN adjunto_entrega a ON a.version_id=v.id
            WHERE e.asignacion_id=? AND e.etapa_id=? ORDER BY v.numero DESC LIMIT 100
            """,(rs,i)->new Version(rs.getInt(1),rs.getString(2),rs.getObject(3,OffsetDateTime.class),rs.getString(4)+" "+rs.getString(5),rs.getObject(6,UUID.class),rs.getString(7)),assignment,a.stage());
        var criteria=db.query("SELECT descripcion FROM criterio_evaluacion WHERE mision_id=? AND (etapa_id IS NULL OR etapa_id=?) ORDER BY orden",(rs,i)->rs.getString(1),a.mission(),a.stage());
        var grades=db.query("""
            SELECT ev.calificacion_base,ev.observacion,v.numero,ai.ajuste,ai.justificacion FROM version_evaluacion ev JOIN evaluacion e ON e.id=ev.evaluacion_id
            JOIN version_entrega v ON v.id=ev.version_entrega_id JOIN entrega_etapa d ON d.id=e.entrega_etapa_id
            LEFT JOIN ajuste_individual ai ON ai.version_evaluacion_id=ev.id AND ai.usuario_id=?
            WHERE d.asignacion_id=? AND d.etapa_id=? ORDER BY ev.numero DESC LIMIT 1
            """,(rs,i)->new Grade(rs.getBigDecimal(1),rs.getString(2),rs.getInt(3),rs.getBigDecimal(4),rs.getString(5)),a.user(),assignment,a.stage());
        return new View(a.title(),a.deadline(),versions,criteria,grades.isEmpty() ? null : grades.getFirst(),stages,a.stage(),a.open());
    }
    @Transactional
    public void send(String email,UUID assignment,String content) { send(email,assignment,null,content,null); }
    @Transactional
    public void send(String email,UUID assignment,UUID stage,String content,org.springframework.web.multipart.MultipartFile attachment) {
        String text=content.strip();
        if(text.isEmpty() || text.length()>20000) throw new IllegalArgumentException("La entrega debe tener entre 1 y 20.000 caracteres.");
        attachments.validate(attachment);
        // Serializa versiones y evita dos contadores idénticos incluso para entregas grupales.
        db.query("SELECT id FROM asignacion_mision WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),assignment);
        Access a=access(email,assignment,stage);
        if(!a.open()) throw new IllegalArgumentException("Esta Misión está cerrada y no admite nuevas entregas.");
        if(a.finalStage()) {
            Integer missing=db.queryForObject("""
                SELECT COUNT(*) FROM etapa_mision e WHERE e.mision_id=? AND e.obligatoria=TRUE AND e.es_final=FALSE
                AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN version_entrega v ON v.entrega_etapa_id=d.id
                                WHERE d.asignacion_id=? AND d.etapa_id=e.id)
                """,Integer.class,a.mission(),assignment);
            if(missing != null && missing>0) throw new IllegalArgumentException("Antes de la entrega final debes enviar todas las etapas obligatorias.");
        }
        OffsetDateTime now=OffsetDateTime.now(ZoneOffset.UTC);
        var existing=db.query("SELECT id FROM entrega_etapa WHERE asignacion_id=? AND etapa_id=?",(rs,i)->rs.getObject(1,UUID.class),assignment,a.stage());
        UUID submission;
        if(existing.isEmpty()) { submission=UUID.randomUUID(); db.update("INSERT INTO entrega_etapa(id,mision_id,asignacion_id,etapa_id) VALUES (?,?,?,?)",submission,a.mission(),assignment,a.stage()); }
        else submission=existing.getFirst();
        UUID authorization=null;
        if(a.deadline()!=null && now.isAfter(a.deadline())) {
            var permissions=db.query("SELECT id FROM autorizacion_tardia WHERE entrega_etapa_id=? AND concedida_en<=? AND (valida_hasta IS NULL OR valida_hasta>=?) AND revocada_en IS NULL ORDER BY concedida_en DESC LIMIT 1",(rs,i)->rs.getObject(1,UUID.class),submission,now,now);
            if(permissions.isEmpty()) throw new IllegalArgumentException("La fecha límite ha vencido. Se requiere autorización docente vigente.");
            authorization=permissions.getFirst();
        }
        Integer number=db.queryForObject("SELECT COALESCE(MAX(numero),0)+1 FROM version_entrega WHERE entrega_etapa_id=?",Integer.class,submission);
        UUID version=UUID.randomUUID();
        db.update("INSERT INTO version_entrega(id,entrega_etapa_id,numero,autor_id,contenido,enviada_en,autorizacion_id) VALUES (?,?,?,?,?,?,?)",version,submission,number,a.user(),text,now,authorization);
        attachments.save(version,attachment);
        audit.record(a.user(),"ENVIAR_ENTREGA","VERSION_ENTREGA",version);
    }
    private record Access(UUID mission,UUID user,String title,UUID stage,OffsetDateTime deadline,boolean finalStage,boolean open) {}
    public record Version(int number,String content,OffsetDateTime sent,String author,UUID attachment,String attachmentName) {}
    public record Grade(java.math.BigDecimal score,String comment,int version,java.math.BigDecimal adjustment,String justification) {}
    public record Stage(UUID id,String title,boolean required) {}
    public record View(String title,OffsetDateTime deadline,List<Version> versions,List<String> criteria,Grade grade,List<Stage> stages,UUID stage,boolean open) {}
}
