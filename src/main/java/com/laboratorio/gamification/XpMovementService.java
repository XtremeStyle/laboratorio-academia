package com.laboratorio.gamification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Libro de movimientos XP: cada evaluación o corrección de una etapa recalcula la
 * contribución XP de la Misión para cada participante y escribe un movimiento con
 * el delta correspondiente (positivo o negativo) respecto a lo ya registrado.
 */
@Service
public class XpMovementService {
    private final JdbcTemplate db;

    public XpMovementService(JdbcTemplate db) {
        this.db = db;
    }

    /** Recalcula los XP de TODOS los participantes activos de una asignación Mision y registra deltas. */
    @Transactional
    public void recalcForAssignment(UUID assignmentId, UUID evaluationId) {
        var participants = db.query("""
                SELECT pa.usuario_id FROM participante_asignacion pa WHERE pa.asignacion_id=? AND pa.retirado_en IS NULL
                """, (rs, i) -> rs.getObject(1, UUID.class), assignmentId);
        for (UUID participant : participants) recalcParticipant(participant, assignmentId, evaluationId);
    }

    /** Recalcula los XP de una asignación para un usuario concreto. */
    @Transactional
    public void recalcParticipant(UUID userId, UUID assignmentId, UUID evaluationId) {
        var rows = db.query("""
                SELECT am.mision_id, am.academia_id
                FROM asignacion_mision am WHERE am.id=?
                """, (rs, i) -> new Object[]{rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)}, assignmentId);
        if (rows.isEmpty()) return;
        Object[] row = rows.getFirst();
        UUID mission = (UUID) row[0], academy = (UUID) row[1];
        String estadoAsignacion = db.queryForObject("SELECT estado FROM asignacion_mision WHERE id=?", String.class, assignmentId);
        String estadoMision = db.queryForObject("SELECT estado FROM mision WHERE id=?", String.class, mission);
        boolean active = "ACTIVA".equals(estadoAsignacion) || "COMPLETADA".equals(estadoAsignacion);
        boolean missionPublic = "PUBLICADA".equals(estadoMision) || "CERRADA".equals(estadoMision);
        // Promedio efectivo sobre las etapas evaluadas, incluyendo el ajuste individual del usuario.
        List<BigDecimal> averages = db.query("""
                SELECT AVG(ev.calificacion_base+COALESCE(ai.ajuste,0))
                FROM entrega_etapa d JOIN evaluacion e ON e.entrega_etapa_id=d.id
                JOIN version_evaluacion ev ON ev.evaluacion_id=e.id AND ev.numero=(SELECT MAX(v2.numero) FROM version_evaluacion v2 WHERE v2.evaluacion_id=e.id)
                LEFT JOIN ajuste_individual ai ON ai.version_evaluacion_id=ev.id AND ai.usuario_id=?
                WHERE d.asignacion_id=? AND ev.calificacion_base+COALESCE(ai.ajuste,0) BETWEEN 0 AND 5
                """, (rs, i) -> rs.getBigDecimal(1), userId, assignmentId);
        Integer pendingStages = db.queryForObject("""
                SELECT COUNT(*) FROM etapa_mision st
                WHERE st.mision_id=? AND st.obligatoria=TRUE
                  AND NOT EXISTS (SELECT 1 FROM entrega_etapa d JOIN version_entrega v ON v.entrega_etapa_id=d.id
                                  WHERE d.asignacion_id=? AND d.etapa_id=st.id)
                """, Integer.class, mission, assignmentId);
        Integer finalEvaluated = db.queryForObject("""
                SELECT COUNT(*) FROM etapa_mision fs
                WHERE fs.mision_id=? AND fs.es_final=TRUE
                  AND EXISTS (SELECT 1 FROM entrega_etapa fd JOIN evaluacion fe ON fe.entrega_etapa_id=fd.id
                    JOIN version_evaluacion fv ON fv.evaluacion_id=fe.id
                    WHERE fd.asignacion_id=? AND fv.numero=(SELECT MAX(fv2.numero) FROM version_evaluacion fv2 WHERE fv2.evaluacion_id=fe.id)
                      AND fv.calificacion_base BETWEEN 0 AND 5)
                """, Integer.class, mission, assignmentId);
        Integer gradeOutOfScale = db.queryForObject("""
                SELECT COUNT(*) FROM version_evaluacion ev JOIN entrega_etapa d ON d.id=ev.entrega_etapa_id
                WHERE d.asignacion_id=? AND (ev.calificacion_base<0 OR ev.calificacion_base>5)
                """, Integer.class, assignmentId);
        boolean eligible = active && missionPublic && (pendingStages == null || pendingStages == 0)
                && (finalEvaluated != null && finalEvaluated == 1)
                && (gradeOutOfScale == null || gradeOutOfScale == 0)
                && averages.size() == 1 && averages.getFirst() != null;
        int missionXp = eligible ? averages.getFirst().multiply(BigDecimal.valueOf(20)).setScale(0, RoundingMode.HALF_UP).intValueExact() : 0;

        Integer previous = db.queryForObject("SELECT COALESCE(SUM(cantidad),0) FROM xp_movimiento WHERE usuario_id=? AND asignacion_id=?", Integer.class, userId, assignmentId);
        int prev = previous != null ? previous : 0;
        int delta = missionXp - prev;
        if (delta == 0) return;
        String motivo = prev == 0 && missionXp >= 0 ? "EVALUACION" : "CORRECCION";
        Integer currentTotalRows = db.queryForObject("SELECT COALESCE(SUM(cantidad),0) FROM xp_movimiento WHERE usuario_id=?", Integer.class, userId);
        int saldo = (currentTotalRows != null ? currentTotalRows : 0) + delta;
        db.update("INSERT INTO xp_movimiento(id,academia_id,usuario_id,asignacion_id,mision_id,evaluacion_id,cantidad,saldo,motivo,creado_en) VALUES (?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                UUID.randomUUID(), academy, userId, assignmentId, mission, evaluationId, delta, saldo, motivo);
    }

    @Transactional(readOnly = true)
    public List<Movement> movementsFor(String email, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("El límite debe estar entre 1 y 100.");
        return db.query("""
                SELECT mov.creado_en, m.titulo, mov.motivo, mov.cantidad, mov.saldo
                FROM xp_movimiento mov
                JOIN mision m ON m.id=mov.mision_id
                JOIN usuario u ON u.id=mov.usuario_id
                WHERE u.correo=?
                ORDER BY mov.creado_en DESC, mov.id DESC
                LIMIT ?
                """, (rs, i) -> new Movement(rs.getObject(1, OffsetDateTime.class), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5)), email, limit);
    }

    @Transactional(readOnly = true)
    public int totalFor(String email) {
        Integer sum = db.queryForObject("SELECT COALESCE(SUM(mov.cantidad),0) FROM xp_movimiento mov JOIN usuario u ON u.id=mov.usuario_id WHERE u.correo=?", Integer.class, email);
        return sum != null ? sum : 0;
    }

    public record Movement(OffsetDateTime at, String mission, String motive, int amount, int balance) { }
}
