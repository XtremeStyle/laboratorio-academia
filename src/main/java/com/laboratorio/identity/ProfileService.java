package com.laboratorio.identity;
import com.laboratorio.audit.AuditService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ProfileService {
    private final JdbcTemplate db; private final PasswordEncoder passwords; private final AuditService audit;
    public ProfileService(JdbcTemplate db,PasswordEncoder passwords,AuditService audit) { this.db=db; this.passwords=passwords; this.audit=audit; }
    @Transactional(readOnly=true)
    public Profile view(String email) {
        var rows=db.query("SELECT id,nombre,apellido,correo FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->new Profile(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),List.of()),email);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var p=rows.getFirst();
        var roles=db.query("SELECT r.nombre FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? ORDER BY r.nombre",(rs,i)->rs.getString(1),p.id());
        return new Profile(p.id(),p.name(),p.surname(),p.email(),roles);
    }
    @Transactional
    public void update(String email,String name,String surname) {
        String first=name.strip(),last=surname.strip();
        if(first.isEmpty() || first.length()>120 || last.isEmpty() || last.length()>120) throw new IllegalArgumentException("Indica nombre y apellido de hasta 120 caracteres.");
        var ids=db.query("SELECT id FROM usuario WHERE correo=? AND estado='ACTIVO' FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),email);
        if(ids.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        db.update("UPDATE usuario SET nombre=?,apellido=? WHERE id=?",first,last,ids.getFirst());
        audit.record(ids.getFirst(),"EDITAR_PERFIL","USUARIO",ids.getFirst());
    }
    @Transactional
    public void changePassword(String email,String current,String next,String confirmation) {
        if(next.length()<12 || next.getBytes(StandardCharsets.UTF_8).length>72 || !next.equals(confirmation)) throw new IllegalArgumentException("La contraseña nueva debe coincidir y tener al menos 12 caracteres y como máximo 72 bytes UTF-8.");
        var rows=db.query("SELECT id,password_hash FROM usuario WHERE correo=? AND estado='ACTIVO' FOR UPDATE",(rs,i)->new Credentials(rs.getObject(1,UUID.class),rs.getString(2)),email);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var credentials=rows.getFirst();
        if(!passwords.matches(current,credentials.hash())) throw new IllegalArgumentException("La contraseña actual no es correcta.");
        if(passwords.matches(next,credentials.hash())) throw new IllegalArgumentException("La contraseña nueva debe ser distinta de la actual.");
        db.update("UPDATE usuario SET password_hash=? WHERE id=?",passwords.encode(next),credentials.id());
        audit.record(credentials.id(),"CAMBIAR_CONTRASENA","USUARIO",credentials.id());
    }
    private record Credentials(UUID id,String hash) {}
    public record Profile(UUID id,String name,String surname,String email,List<String> roles) {}
}
