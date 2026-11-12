package com.laboratorio.progress;

import com.laboratorio.notification.NotificationService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PerformanceAlertService {
    private final JdbcTemplate db; private final ProgressService progress; private final NotificationService notifications;
    public PerformanceAlertService(JdbcTemplate db,ProgressService progress,NotificationService notifications) { this.db=db; this.progress=progress; this.notifications=notifications; }
    /** Debe ejecutarse en la misma transacción que cambia una evaluación. */
    public void reconcile(UUID assignment) {
        var users=db.query("SELECT u.id,u.correo FROM participante_asignacion pa JOIN usuario u ON u.id=pa.usuario_id WHERE pa.asignacion_id=? AND pa.retirado_en IS NULL AND u.estado='ACTIVO' ORDER BY u.id",(rs,i)->new Person(rs.getObject(1,UUID.class),rs.getString(2)),assignment);
        for(Person user:users) reconcilePerson(user);
    }
    @org.springframework.transaction.annotation.Transactional
    public void reconcileUser(String email) {
        var people=db.query("SELECT id,correo FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->new Person(rs.getObject(1,UUID.class),rs.getString(2)),email);
        if(people.size()==1) reconcilePerson(people.getFirst());
    }
    private void reconcilePerson(Person user) {
        db.query("SELECT id FROM usuario WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),user.id());
        Map<Key,ProgressService.Competency> low=new HashMap<>();
        for(var c:progress.mine(user.email())) if(c.average().compareTo(BigDecimal.valueOf(3))<0) low.put(new Key(c.id(),c.trackId()),c);
        List<Open> open=db.query("SELECT id,competencia_id,semillero_id FROM alerta_rendimiento WHERE usuario_id=? AND tipo='COMPETENCIA_BAJA' AND estado='ABIERTA'",(rs,i)->new Open(rs.getObject(1,UUID.class),new Key(rs.getObject(2,UUID.class),rs.getObject(3,UUID.class))),user.id());
        for(Open alert:open) {
            if(low.remove(alert.key())==null) {
                db.update("UPDATE alerta_rendimiento SET estado='RESUELTA',resuelta_en=CURRENT_TIMESTAMP WHERE id=? AND estado='ABIERTA'",alert.id());
                notifications.notifyUser(user.id(),"ALERTA_RESUELTA","Una alerta de competencia fue resuelta. Consulta Mi progreso para ver el estado actual.");
            }
        }
        for(var item:low.entrySet()) {
            UUID id=UUID.randomUUID(); var c=item.getValue();
            db.update("INSERT INTO alerta_rendimiento(id,usuario_id,competencia_id,semillero_id,tipo,estado,generada_en,detalle) VALUES (?,?,?,?,'COMPETENCIA_BAJA','ABIERTA',CURRENT_TIMESTAMP,?)",id,user.id(),c.id(),c.trackId(),"Promedio inferior a 3,0.");
            notifications.notifyUser(user.id(),"ALERTA_COMPETENCIA","Una competencia requiere atención. Consulta Mi progreso y habla con tu docente.");
        }
    }
    private record Person(UUID id,String email) {}
    private record Key(UUID competency,UUID track) {}
    private record Open(UUID id,Key key) {}
}
