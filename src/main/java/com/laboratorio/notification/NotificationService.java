package com.laboratorio.notification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final JdbcTemplate db;
    public NotificationService(JdbcTemplate db) { this.db=db; }

    /** Se ejecuta en la transacción del evento académico. */
    public void notifyAssignment(UUID assignment,String type,String message) {
        var recipients=db.query("SELECT p.usuario_id FROM participante_asignacion p JOIN usuario u ON u.id=p.usuario_id WHERE p.asignacion_id=? AND p.retirado_en IS NULL AND u.estado='ACTIVO'",(rs,i)->rs.getObject(1,UUID.class),assignment);
        for(UUID recipient:recipients) db.update("INSERT INTO notificacion(id,destinatario_id,tipo,contenido,creada_en) VALUES (?,?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),recipient,type,message);
    }
    public void notifyUser(UUID recipient,String type,String message) {
        db.update("INSERT INTO notificacion(id,destinatario_id,tipo,contenido,creada_en) VALUES (?,?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),recipient,type,message);
    }
    @Transactional(readOnly=true)
    public List<Notice> list(String email) {
        return db.query("SELECT n.id,n.contenido,n.creada_en,n.leida_en FROM notificacion n JOIN usuario u ON u.id=n.destinatario_id WHERE u.correo=? AND u.estado='ACTIVO' ORDER BY n.creada_en DESC,n.id LIMIT 100",(rs,i)->new Notice(rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,OffsetDateTime.class),rs.getObject(4,OffsetDateTime.class)),email);
    }
    @Transactional
    public void markRead(String email,UUID id) {
        int updated=db.update("UPDATE notificacion SET leida_en=COALESCE(leida_en,CURRENT_TIMESTAMP) WHERE id=? AND destinatario_id IN (SELECT id FROM usuario WHERE correo=? AND estado='ACTIVO')",id,email);
        if(updated==0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
    }
    public record Notice(UUID id,String content,OffsetDateTime created,OffsetDateTime read) {}
}
