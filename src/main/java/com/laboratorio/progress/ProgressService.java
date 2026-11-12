package com.laboratorio.progress;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ProgressService {
    private final JdbcTemplate db;
    public ProgressService(JdbcTemplate db) { this.db=db; }
    @Transactional(readOnly=true)
    public List<Competency> mine(String email) {
        return db.query("""
          SELECT c.nombre,s.nombre,AVG(pc.puntaje),COUNT(*),c.id,s.id FROM usuario u
          JOIN participante_asignacion pa ON pa.usuario_id=u.id AND pa.retirado_en IS NULL
          JOIN asignacion_mision am ON am.id=pa.asignacion_id AND am.estado IN ('ACTIVA','COMPLETADA')
          JOIN entrega_etapa d ON d.asignacion_id=am.id JOIN evaluacion e ON e.entrega_etapa_id=d.id
          JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
          JOIN puntaje_criterio pc ON pc.version_evaluacion_id=ev.id
          JOIN criterio_competencia cc ON cc.criterio_id=pc.criterio_id JOIN competencia c ON c.id=cc.competencia_id AND c.estado='ACTIVA'
          JOIN mision_semillero ms ON ms.mision_id=am.mision_id AND ms.semillero_id=am.semillero_destino_id JOIN competencia_semillero cs ON cs.competencia_id=c.id AND cs.semillero_id=ms.semillero_id
          JOIN semillero s ON s.id=cs.semillero_id
          WHERE u.correo=? AND u.estado='ACTIVO' AND pc.puntaje BETWEEN 0 AND 5
          GROUP BY c.id,c.nombre,s.id,s.nombre ORDER BY s.nombre,c.nombre
          """,(rs,i)->new Competency(rs.getString(1),rs.getString(2),rs.getBigDecimal(3),rs.getInt(4),rs.getObject(5,java.util.UUID.class),rs.getObject(6,java.util.UUID.class)),email);
    }
    @Transactional(readOnly=true)
    public List<Report> report(String email) {
        var actors=db.query("SELECT u.id,u.academia_id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO' AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo IN ('PROFESOR','ADMINISTRADOR'))",(rs,i)->new Actor(rs.getObject(1,java.util.UUID.class),rs.getObject(2,java.util.UUID.class)),email);
        if(actors.size()!=1) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        Actor actor=actors.getFirst();
        return db.query("""
          SELECT u.nombre,u.apellido,c.nombre,s.nombre,AVG(pc.puntaje),COUNT(*) FROM usuario u
          JOIN participante_asignacion pa ON pa.usuario_id=u.id AND pa.retirado_en IS NULL
          JOIN asignacion_mision am ON am.id=pa.asignacion_id AND am.estado IN ('ACTIVA','COMPLETADA')
          JOIN entrega_etapa d ON d.asignacion_id=am.id JOIN evaluacion e ON e.entrega_etapa_id=d.id
          JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
          JOIN puntaje_criterio pc ON pc.version_evaluacion_id=ev.id JOIN criterio_competencia cc ON cc.criterio_id=pc.criterio_id
          JOIN competencia c ON c.id=cc.competencia_id AND c.estado='ACTIVA' JOIN mision_semillero ms ON ms.mision_id=am.mision_id AND ms.semillero_id=am.semillero_destino_id
          JOIN competencia_semillero cs ON cs.competencia_id=c.id AND cs.semillero_id=ms.semillero_id JOIN semillero s ON s.id=cs.semillero_id
          WHERE u.academia_id=? AND u.estado='ACTIVO' AND pc.puntaje BETWEEN 0 AND 5 AND
            (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR')
              OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=s.id))
          GROUP BY u.id,u.nombre,u.apellido,c.id,c.nombre,s.id,s.nombre ORDER BY s.nombre,u.apellido,u.nombre,c.nombre LIMIT 500
          """,(rs,i)->new Report(rs.getString(1)+" "+rs.getString(2),rs.getString(3),rs.getString(4),rs.getBigDecimal(5),rs.getInt(6)),actor.academy(),actor.id(),actor.id());
    }
    private record Actor(java.util.UUID id,java.util.UUID academy) {}
    public record Report(String student,String competency,String track,BigDecimal average,int count) {}
    public record Competency(String name,String track,BigDecimal average,int count,java.util.UUID id,java.util.UUID trackId) {}
}
