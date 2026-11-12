package com.laboratorio.audit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditService {
    private final JdbcTemplate db;
    public AuditService(JdbcTemplate db) { this.db=db; }
    /** Registrar en la misma transacción que la operación; no incluir entregas ni contraseñas. */
    public void record(UUID actor,String action,String resource,UUID id) {
        db.update("INSERT INTO auditoria(id,actor_id,accion,recurso_tipo,recurso_id,ocurrido_en,resultado) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP,'EXITO')",UUID.randomUUID(),actor,action,resource,id);
    }
    @Transactional(readOnly=true)
    public List<Entry> list(String email) {
        var academies=db.query("SELECT u.academia_id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO' AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ADMINISTRADOR')",(rs,i)->rs.getObject(1,UUID.class),email);
        if(academies.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return db.query("SELECT a.ocurrido_en,u.correo,a.accion,a.recurso_tipo,a.recurso_id FROM auditoria a JOIN usuario u ON u.id=a.actor_id WHERE u.academia_id=? ORDER BY a.ocurrido_en DESC,a.id LIMIT 200",(rs,i)->new Entry(rs.getObject(1,OffsetDateTime.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getObject(5,UUID.class)),academies.getFirst());
    }
    public record Entry(OffsetDateTime time,String actor,String action,String resource,UUID resourceId) {}
}
