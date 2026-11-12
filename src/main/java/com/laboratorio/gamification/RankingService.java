package com.laboratorio.gamification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** XP calculado desde las evaluaciones vigentes, sin duplicar puntos al corregir notas. */
@Service
public class RankingService {
    private final JdbcTemplate db;
    public RankingService(JdbcTemplate db) { this.db=db; }

    @Transactional(readOnly=true)
    public List<Track> tracks(String email) {
        return db.query("""
            SELECT s.id,s.nombre FROM semillero s JOIN usuario u ON u.academia_id=s.academia_id
            WHERE u.correo=? AND u.estado='ACTIVO' AND s.estado='ACTIVO' AND
              (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ADMINISTRADOR')
               OR EXISTS (SELECT 1 FROM asignacion_semillero a JOIN asignacion_activa aa ON aa.asignacion_id=a.id WHERE a.usuario_id=u.id AND a.semillero_id=s.id))
            ORDER BY s.nombre
            """,(rs,i)->new Track(rs.getObject(1,UUID.class),rs.getString(2)),email);
    }

    @Transactional(readOnly=true)
    public Ranking ranking(String email,UUID trackId) {
        Track track=tracks(email).stream().filter(t->t.id().equals(trackId)).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        // Solo Misiones con entrega final evaluada y todas las etapas obligatorias entregadas.
        // Cada corrección reemplaza la nota anterior en el cálculo; no crea XP duplicado.
        var rows=db.query("""
            SELECT u.id,u.nombre,u.apellido,am.id,
              (SELECT AVG(ev.calificacion_base+COALESCE(ai.ajuste,0)) FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
               JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
               LEFT JOIN ajuste_individual ai ON ai.version_evaluacion_id=ev.id AND ai.usuario_id=u.id
               WHERE d.asignacion_id=am.id AND ev.calificacion_base+COALESCE(ai.ajuste,0) BETWEEN 0 AND 5)
            FROM asignacion_mision am JOIN mision m ON m.id=am.mision_id
            JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
            JOIN participante_asignacion pa ON pa.asignacion_id=am.id AND pa.retirado_en IS NULL
            JOIN usuario u ON u.id=pa.usuario_id AND u.estado='ACTIVO'
            JOIN asignacion_semillero a ON a.usuario_id=u.id AND a.semillero_id=ms.semillero_id
            JOIN asignacion_activa aa ON aa.asignacion_id=a.id
            WHERE ms.semillero_id=? AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE') AND am.estado IN ('ACTIVA','COMPLETADA') AND m.estado IN ('PUBLICADA','CERRADA')
              AND EXISTS (SELECT 1 FROM entrega_etapa fd JOIN etapa_mision fs ON fs.id=fd.etapa_id AND fs.es_final=TRUE
                JOIN evaluacion fe ON fe.entrega_etapa_id=fd.id JOIN version_evaluacion fv ON fv.evaluacion_id=fe.id
                WHERE fd.asignacion_id=am.id AND fv.numero=(SELECT MAX(fv2.numero) FROM version_evaluacion fv2 WHERE fv2.evaluacion_id=fe.id)
                  AND fv.calificacion_base BETWEEN 0 AND 5)
              AND NOT EXISTS (SELECT 1 FROM etapa_mision st WHERE st.mision_id=m.id AND st.obligatoria=TRUE
                AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN version_entrega v ON v.entrega_etapa_id=d.id WHERE d.asignacion_id=am.id AND d.etapa_id=st.id))
              AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
                JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
                WHERE d.asignacion_id=am.id AND (ev.calificacion_base<0 OR ev.calificacion_base>5))
            """,(rs,i)->new Result(rs.getObject(1,UUID.class),rs.getString(2)+" "+rs.getString(3),rs.getBigDecimal(5)),trackId);
        return new Ranking(track,aggregate(rows));
    }
    @Transactional(readOnly=true)
    public List<Entry> global(String email) {
        var academies=db.query("SELECT u.academia_id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO' AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ADMINISTRADOR')",(rs,i)->rs.getObject(1,UUID.class),email);
        if(academies.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        UUID academy=academies.getFirst();
        var rows=db.query("""
            SELECT DISTINCT u.id,u.nombre,u.apellido,am.id,
              (SELECT AVG(ev.calificacion_base+COALESCE(ai.ajuste,0)) FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
               JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
               LEFT JOIN ajuste_individual ai ON ai.version_evaluacion_id=ev.id AND ai.usuario_id=u.id
               WHERE d.asignacion_id=am.id AND ev.calificacion_base+COALESCE(ai.ajuste,0) BETWEEN 0 AND 5)
            FROM asignacion_mision am JOIN mision m ON m.id=am.mision_id
            JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
            JOIN participante_asignacion pa ON pa.asignacion_id=am.id AND pa.retirado_en IS NULL
            JOIN usuario u ON u.id=pa.usuario_id AND u.estado='ACTIVO'
            WHERE u.academia_id=? AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE') AND am.estado IN ('ACTIVA','COMPLETADA') AND m.estado IN ('PUBLICADA','CERRADA')
              AND EXISTS (SELECT 1 FROM entrega_etapa fd JOIN etapa_mision fs ON fs.id=fd.etapa_id AND fs.es_final=TRUE
                JOIN evaluacion fe ON fe.entrega_etapa_id=fd.id JOIN version_evaluacion fv ON fv.evaluacion_id=fe.id
                WHERE fd.asignacion_id=am.id AND fv.numero=(SELECT MAX(fv2.numero) FROM version_evaluacion fv2 WHERE fv2.evaluacion_id=fe.id)
                  AND fv.calificacion_base BETWEEN 0 AND 5)
              AND NOT EXISTS (SELECT 1 FROM etapa_mision st WHERE st.mision_id=m.id AND st.obligatoria=TRUE
                AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN version_entrega v ON v.entrega_etapa_id=d.id WHERE d.asignacion_id=am.id AND d.etapa_id=st.id))
              AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
                JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
                WHERE d.asignacion_id=am.id AND (ev.calificacion_base<0 OR ev.calificacion_base>5))
            """,(rs,i)->new Result(rs.getObject(1,UUID.class),rs.getString(2)+" "+rs.getString(3),rs.getBigDecimal(5)),academy);
        return aggregate(rows);
    }
    private List<Entry> aggregate(List<Result> rows) {
        Map<UUID,Accumulator> totals=new LinkedHashMap<>();
        for(Result row:rows) {
            if(row.average()==null) continue;
            Accumulator a=totals.computeIfAbsent(row.user(),id->new Accumulator(row.name()));
            a.xp+=row.average().multiply(BigDecimal.valueOf(20)).setScale(0,RoundingMode.HALF_UP).intValueExact();
            a.scores=a.scores.add(row.average()); a.missions++;
        }
        List<Entry> entries=new ArrayList<>();
        totals.forEach((id,a)->entries.add(new Entry(a.name,a.xp,a.missions,a.scores.divide(BigDecimal.valueOf(a.missions),2,RoundingMode.HALF_UP))));
        entries.sort(Comparator.comparingInt(Entry::xp).reversed().thenComparing(Entry::average,Comparator.reverseOrder()).thenComparing(Entry::name));
        return List.copyOf(entries);
    }
    @Transactional(readOnly=true)
    public Personal personal(String email) {
        var averages=db.query("""
            SELECT DISTINCT am.id,
              (SELECT AVG(ev.calificacion_base+COALESCE(ai.ajuste,0)) FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
               JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
               LEFT JOIN ajuste_individual ai ON ai.version_evaluacion_id=ev.id AND ai.usuario_id=u.id
               WHERE d.asignacion_id=am.id AND ev.calificacion_base+COALESCE(ai.ajuste,0) BETWEEN 0 AND 5)
            FROM asignacion_mision am JOIN mision m ON m.id=am.mision_id
            JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id
            JOIN participante_asignacion pa ON pa.asignacion_id=am.id
            JOIN usuario u ON u.id=pa.usuario_id
            WHERE u.correo=? AND am.estado IN ('ACTIVA','COMPLETADA') AND m.estado IN ('PUBLICADA','CERRADA')
              AND EXISTS (SELECT 1 FROM entrega_etapa fd JOIN etapa_mision fs ON fs.id=fd.etapa_id AND fs.es_final=TRUE
                JOIN evaluacion fe ON fe.entrega_etapa_id=fd.id JOIN version_evaluacion fv ON fv.evaluacion_id=fe.id
                WHERE fd.asignacion_id=am.id AND fv.numero=(SELECT MAX(fv2.numero) FROM version_evaluacion fv2 WHERE fv2.evaluacion_id=fe.id)
                  AND fv.calificacion_base BETWEEN 0 AND 5)
              AND NOT EXISTS (SELECT 1 FROM etapa_mision st WHERE st.mision_id=m.id AND st.obligatoria=TRUE
                AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN version_entrega v ON v.entrega_etapa_id=d.id WHERE d.asignacion_id=am.id AND d.etapa_id=st.id))
              AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
                JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
                WHERE d.asignacion_id=am.id AND (ev.calificacion_base<0 OR ev.calificacion_base>5))
            """,(rs,i)->rs.getBigDecimal(2),email);
        int xp=0;
        for(BigDecimal average:averages) if(average!=null) xp+=average.multiply(BigDecimal.valueOf(20)).setScale(0,RoundingMode.HALF_UP).intValueExact();
        String rank=xp>=3000 ? "Mentor" : xp>=1500 ? "Especialista" : xp>=500 ? "Investigador" : "Explorador";
        return new Personal(xp,rank,averages.size(),averages.stream().anyMatch(a->a!=null && a.compareTo(BigDecimal.valueOf(5))==0));
    }
    public record Personal(int xp,String rank,int missions,boolean perfect) {}
    private record Result(UUID user,String name,BigDecimal average) {}
    private static class Accumulator { final String name; int xp; int missions; BigDecimal scores=BigDecimal.ZERO; Accumulator(String name) { this.name=name; } }
    public record Track(UUID id,String name) {}
    public record Entry(String name,int xp,int missions,BigDecimal average) {}
    public record Ranking(Track track,List<Entry> entries) {}
}
