package com.laboratorio.identity;

import com.laboratorio.domain.Rol;
import com.laboratorio.domain.SolicitudCuenta;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitudService {
    private final com.laboratorio.repository.UsuarioRepository usuarios;
    private static final Set<String> ROLES_SOLICITABLES = Set.of("ESTUDIANTE", "PROFESOR");
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_TEMPORAL = 14;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final com.laboratorio.audit.AuditService audit;

    public SolicitudService(JdbcTemplate jdbc, PasswordEncoder encoder, com.laboratorio.audit.AuditService audit, com.laboratorio.repository.UsuarioRepository usuarios) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.audit = audit;
        this.usuarios = usuarios;
    }

    public enum ResultadoSolicitud { ENVIADA, DUPLICADA, SIN_ACADEMIA }

    /** Registro público: no revela qué correos ya tienen cuenta ni si son reales; las altas las aprueba un administrador. */
    @Transactional
    public ResultadoSolicitud solicitar(String correo, String nombre, String apellido, String rol, String mensaje) {
        Rol rolSolicitado;
        try {
            rolSolicitado = Rol.valueOf(rol == null ? "" : rol.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Solo se pueden solicitar los roles ESTUDIANTE o PROFESOR.");
        }
        if (rolSolicitado == Rol.ADMINISTRADOR) throw new IllegalArgumentException("Solo se pueden solicitar los roles ESTUDIANTE o PROFESOR.");
        var academias = jdbc.query("SELECT id FROM academia WHERE estado='ACTIVA' ORDER BY creado_en LIMIT 1", (rs, i) -> rs.getObject(1, UUID.class));
        if (academias.isEmpty()) return ResultadoSolicitud.SIN_ACADEMIA;
        UUID academia = academias.getFirst();
        String correoNormalizado = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        if (usuarios.existsByCorreo(correoNormalizado)) return ResultadoSolicitud.DUPLICADA; // sin filtrar existencia de la cuenta
        try {
            SolicitudCuenta solicitud = new SolicitudCuenta(UUID.randomUUID(), academia, correo, nombre, apellido,
                    rolSolicitado, mensaje, SolicitudCuenta.EstadoSolicitud.PENDIENTE, OffsetDateTime.now(ZoneOffset.UTC));
            jdbc.update("INSERT INTO solicitud_cuenta(id,academia_id,correo,nombre,apellido,rol_solicitado,mensaje,estado,creada_en) VALUES (?,?,?,?,?,?,?,'PENDIENTE',?)",
                    solicitud.id(), solicitud.academiaId(), solicitud.correo(), solicitud.nombre(), solicitud.apellido(), solicitud.rolSolicitado().name(), solicitud.mensaje(), solicitud.creadaEn());
            return ResultadoSolicitud.ENVIADA;
        } catch (org.springframework.dao.DuplicateKeyException duplicada) {
            return ResultadoSolicitud.DUPLICADA; // re-solicitud del mismo correo
        }
    }

    private UUID academiaAdministrada(String actor) {
        var rows = jdbc.query("SELECT u.academia_id FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'",
                (rs, i) -> rs.getObject(1, UUID.class), actor);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return rows.getFirst();
    }

    @Transactional(readOnly = true)
    public List<SolicitudVista> listar(String actor) {
        UUID academia = academiaAdministrada(actor);
        return jdbc.query("""
                SELECT id,correo,nombre,apellido,rol_solicitado,mensaje,estado,creada_en
                FROM solicitud_cuenta WHERE academia_id=?
                ORDER BY CASE estado WHEN 'PENDIENTE' THEN 0 ELSE 1 END, creada_en DESC LIMIT 100
                """, (rs, i) -> {
                    var creada = rs.getObject(8, OffsetDateTime.class);
                    return new SolicitudVista(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), creada == null ? "" : creada.toLocalDateTime().format(FECHA));
                }, academia);
    }

    @Transactional
    public Aprobacion aprobar(String actor, UUID solicitudId) {
        UUID academia = academiaAdministrada(actor);
        var adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE correo=?", UUID.class, actor);
        var rows = jdbc.query("SELECT correo,nombre,apellido,rol_solicitado FROM solicitud_cuenta WHERE id=? AND academia_id=? FOR UPDATE",
                (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)}, solicitudId, academia);
        if (rows.size() != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Object[] row = rows.getFirst();
        String correo = (String) row[0], nombre = (String) row[1], apellido = (String) row[2], rol = (String) row[3];
        var existentes = jdbc.query("SELECT id FROM usuario WHERE correo=? FOR UPDATE", (rs, i) -> rs.getObject(1, UUID.class), correo);
        if (!existentes.isEmpty()) throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");

        String temporal = generarContrasenaTemporal();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,?,?,?,'ACTIVO')",
                id, academia, correo, nombre, apellido, encoder.encode(temporal));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo=?", UUID.randomUUID(), id, rol);
        int updated = jdbc.update("UPDATE solicitud_cuenta SET estado='APROBADA', resuelta_en=CURRENT_TIMESTAMP, resuelta_por=? WHERE id=? AND academia_id=? AND estado='PENDIENTE'", adminId, solicitudId, academia);
        if (updated != 1) throw new IllegalStateException("La solicitud ya no está pendiente.");
        audit.record(adminId, "APROBAR_SOLICITUD", "SOLICITUD_CUENTA", solicitudId);
        return new Aprobacion(id, correo, nombre, apellido, rol, temporal);
    }

    @Transactional
    public void rechazar(String actor, UUID solicitudId) {
        UUID academia = academiaAdministrada(actor);
        var adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE correo=?", UUID.class, actor);
        int updated = jdbc.update("UPDATE solicitud_cuenta SET estado='RECHAZADA', resuelta_en=CURRENT_TIMESTAMP, resuelta_por=? WHERE id=? AND academia_id=? AND estado='PENDIENTE'", adminId, solicitudId, academia);
        if (updated != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        audit.record(adminId, "RECHAZAR_SOLICITUD", "SOLICITUD_CUENTA", solicitudId);
    }

    private String generarContrasenaTemporal() {
        SecureRandom random = new SecureRandom();
        StringBuilder out = new StringBuilder(LONGITUD_TEMPORAL);
        for (int i = 0; i < LONGITUD_TEMPORAL; i++) out.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        return out.toString();
    }

    public record SolicitudVista(UUID id, String correo, String nombre, String apellido, String rol, String mensaje, String estado, String fechaCreacion) { }

    public record Aprobacion(UUID id, String correo, String nombre, String apellido, String rol, String contrasenaTemporal) {
        public String nombreCompleto() { return nombre + " " + apellido; }
    }
}
