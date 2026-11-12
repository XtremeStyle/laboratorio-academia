package com.laboratorio.presentation.web;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final JdbcTemplate db;
    public DashboardService(JdbcTemplate db) { this.db=db; }
    @Transactional(readOnly=true)
    public Dashboard view(String email) {
        var rows=db.query("SELECT id,nombre FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->new Person(rs.getObject(1,java.util.UUID.class),rs.getString(2)),email);
        if(rows.isEmpty()) return new Dashboard("",0,0,0,List.of());
        Person person=rows.getFirst();
        int missions=db.queryForObject("SELECT COUNT(*) FROM participante_asignacion p JOIN asignacion_mision a ON a.id=p.asignacion_id JOIN mision m ON m.id=a.mision_id WHERE p.usuario_id=? AND p.retirado_en IS NULL AND a.estado='ACTIVA' AND m.estado='PUBLICADA'",Integer.class,person.id());
        int deliveries=db.queryForObject("SELECT COUNT(DISTINCT d.id) FROM participante_asignacion p JOIN entrega_etapa d ON d.asignacion_id=p.asignacion_id WHERE p.usuario_id=? AND p.retirado_en IS NULL AND EXISTS (SELECT 1 FROM version_entrega v WHERE v.entrega_etapa_id=d.id)",Integer.class,person.id());
        int unread=db.queryForObject("SELECT COUNT(*) FROM notificacion WHERE destinatario_id=? AND leida_en IS NULL",Integer.class,person.id());
        var tracks=db.query("SELECT s.nombre FROM asignacion_semillero a JOIN asignacion_activa aa ON aa.asignacion_id=a.id JOIN semillero s ON s.id=a.semillero_id WHERE a.usuario_id=? AND s.estado='ACTIVO' ORDER BY s.nombre",(rs,i)->rs.getString(1),person.id());
        return new Dashboard(person.name(),missions,deliveries,unread,tracks);
    }
    private record Person(java.util.UUID id,String name) {}
    public record Dashboard(String name,int missions,int deliveries,int unread,List<String> tracks) {}
}
