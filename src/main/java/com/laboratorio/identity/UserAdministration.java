package com.laboratorio.identity;

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
public class UserAdministration {
    private static final Set<String> ROLES = Set.of("ADMINISTRADOR", "PROFESOR", "ESTUDIANTE");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final com.laboratorio.audit.AuditService audit;
    public UserAdministration(JdbcTemplate jdbc, PasswordEncoder encoder,com.laboratorio.audit.AuditService audit) { this.jdbc = jdbc; this.encoder = encoder; this.audit=audit; }

    @Transactional(readOnly = true)
    public List<UserRow> listar(String actor) {
        UUID academy = academiaAdministrada(actor);
        var users=jdbc.query("SELECT correo,nombre,apellido,estado,id FROM usuario WHERE academia_id=? ORDER BY apellido,nombre,correo LIMIT 100",
                (rs, i) -> new UserRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),rs.getObject(5,UUID.class),Set.of()), academy);
        var roles=jdbc.query("SELECT ur.usuario_id,r.codigo FROM usuario_rol ur JOIN usuario u ON u.id=ur.usuario_id JOIN rol r ON r.id=ur.rol_id WHERE u.academia_id=?",(rs,i)->new RoleRow(rs.getObject(1,UUID.class),rs.getString(2)),academy).stream().collect(java.util.stream.Collectors.groupingBy(RoleRow::user,java.util.stream.Collectors.mapping(RoleRow::role,java.util.stream.Collectors.toSet())));
        return users.stream().map(u->new UserRow(u.correo(),u.nombre(),u.apellido(),u.estado(),u.id(),roles.getOrDefault(u.id(),Set.of()))).toList();
    }

    @Transactional
    public void crear(String actor, String email, String nombre, String apellido, String password, List<String> roles) {
        UUID academy = academiaAdministrada(actor);
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        String first = nombre.strip(), last = apellido.strip();
        if (normalized.length() > 254 || !normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ||
                first.isEmpty() || first.length() > 120 || last.isEmpty() || last.length() > 120 ||
                password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 ||
                roles == null || roles.isEmpty() || !ROLES.containsAll(roles)) {
            throw new IllegalArgumentException("Revisa correo, nombre, apellido, contraseña y roles. La contraseña requiere al menos 12 caracteres y como máximo 72 bytes UTF-8.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,?,?,?,'ACTIVO')",
                id, academy, normalized, first, last, encoder.encode(password));
        for (String role : Set.copyOf(roles)) {
            int inserted = jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo=?", UUID.randomUUID(), id, role);
            if (inserted != 1) throw new IllegalStateException("Falta un rol en la base de datos");
        }
        audit.record(jdbc.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor),"CREAR_USUARIO","USUARIO",id);
    }
    @Transactional
    public void editarNombre(String actor,UUID user,String nombre,String apellido) {
        UUID academy=academiaAdministrada(actor);
        String first=nombre.strip(),last=apellido.strip();
        if(first.isEmpty() || first.length()>120 || last.isEmpty() || last.length()>120) throw new IllegalArgumentException("Indica nombre y apellido de hasta 120 caracteres.");
        int changed=jdbc.update("UPDATE usuario SET nombre=?,apellido=? WHERE id=? AND academia_id=?",first,last,user,academy);
        if(changed!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        audit.record(jdbc.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor),"EDITAR_USUARIO","USUARIO",user);
    }
    @Transactional
    public void cambiarEstado(String actor,UUID user,String state) {
        UUID academy=academiaAdministrada(actor);
        if(!Set.of("ACTIVO","INACTIVO").contains(state)) throw new IllegalArgumentException("Estado no válido.");
        jdbc.query("SELECT id FROM academia WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),academy);
        academiaAdministrada(actor);
        var rows=jdbc.query("SELECT estado FROM usuario WHERE id=? AND academia_id=? FOR UPDATE",(rs,i)->rs.getString(1),user,academy);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(rows.getFirst().equals("BLOQUEADO")) throw new IllegalArgumentException("Una cuenta bloqueada requiere un procedimiento de desbloqueo separado.");
        if(rows.getFirst().equals(state)) throw new IllegalArgumentException("La cuenta ya tiene ese estado.");
        if(state.equals("INACTIVO")) {
            Integer targetAdmin=jdbc.queryForObject("SELECT COUNT(*) FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR'",Integer.class,user);
            Integer admins=jdbc.queryForObject("SELECT COUNT(DISTINCT u.id) FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.academia_id=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'",Integer.class,academy);
            if(targetAdmin!=null && targetAdmin>0 && (admins==null || admins<=1)) throw new IllegalArgumentException("No puedes desactivar el último administrador activo de la academia.");
        }
        jdbc.update("UPDATE usuario SET estado=? WHERE id=?",state,user);
        audit.record(jdbc.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor),state.equals("ACTIVO") ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO","USUARIO",user);
    }
    @Transactional
    public void cambiarRoles(String actor,UUID user,List<String> requested) {
        UUID academy=academiaAdministrada(actor);
        if(requested==null || requested.isEmpty() || !ROLES.containsAll(requested)) throw new IllegalArgumentException("Selecciona al menos un rol válido.");
        Set<String> desired=Set.copyOf(requested);
        jdbc.query("SELECT id FROM academia WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),academy);
        academiaAdministrada(actor);
        var states=jdbc.query("SELECT estado FROM usuario WHERE id=? AND academia_id=? FOR UPDATE",(rs,i)->rs.getString(1),user,academy);
        if(states.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Set<String> current=new java.util.HashSet<>(jdbc.query("SELECT r.codigo FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=?",(rs,i)->rs.getString(1),user));
        if(current.contains("ADMINISTRADOR") && !desired.contains("ADMINISTRADOR") && states.getFirst().equals("ACTIVO")) {
            Integer admins=jdbc.queryForObject("SELECT COUNT(DISTINCT u.id) FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.academia_id=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'",Integer.class,academy);
            if(admins==null || admins<=1) throw new IllegalArgumentException("No puedes quitar el rol del último administrador activo de la academia.");
        }
        if(current.contains("ESTUDIANTE") && !desired.contains("ESTUDIANTE")) {
            Integer active=jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM participante_asignacion pa JOIN asignacion_mision am ON am.id=pa.asignacion_id WHERE pa.usuario_id=? AND pa.retirado_en IS NULL AND am.estado='ACTIVA') + (SELECT COUNT(*) FROM membresia_tripulacion m JOIN membresia_activa ma ON ma.membresia_id=m.id WHERE m.usuario_id=?)",Integer.class,user,user);
            if(active!=null && active>0) throw new IllegalArgumentException("Primero cierra las membresías y Misiones activas del estudiante.");
        }
        for(String role:current) if(!desired.contains(role)) jdbc.update("DELETE FROM usuario_rol WHERE usuario_id=? AND rol_id=(SELECT id FROM rol WHERE codigo=?)",user,role);
        for(String role:desired) if(!current.contains(role)) jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo=?",UUID.randomUUID(),user,role);
        audit.record(jdbc.queryForObject("SELECT id FROM usuario WHERE correo=?",UUID.class,actor),"CAMBIAR_ROLES","USUARIO",user);
    }
    private UUID academiaAdministrada(String actor) {
        var rows = jdbc.query("SELECT u.academia_id FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'",
                (rs, i) -> rs.getObject(1, UUID.class), actor);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return rows.getFirst();
    }
    private record RoleRow(UUID user,String role) {}
    public record UserRow(String correo, String nombre, String apellido, String estado,UUID id,Set<String> roles) { }
}
