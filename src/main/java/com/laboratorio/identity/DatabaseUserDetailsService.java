package com.laboratorio.identity;

import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    public DatabaseUserDetailsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String email = username.strip().toLowerCase(Locale.ROOT);
        var rows = jdbc.query("SELECT correo,password_hash,estado FROM usuario WHERE correo = ?",
                (rs, i) -> new Credentials(rs.getString(1), rs.getString(2), rs.getString(3)), email);
        if (rows.isEmpty()) throw new UsernameNotFoundException("Credenciales inválidas");
        var credentials = rows.getFirst();
        var roles = jdbc.query("""
                SELECT r.codigo FROM rol r JOIN usuario_rol ur ON ur.rol_id = r.id
                JOIN usuario u ON u.id = ur.usuario_id WHERE u.correo = ?
                """, (rs, i) -> new SimpleGrantedAuthority("ROLE_" + rs.getString(1)), email);
        return new SessionUser(credentials.email(),credentials.password(),"ACTIVO".equals(credentials.status()),roles);
    }
    public static final class SessionUser extends User {
        private final String authenticatedHash;
        public SessionUser(String email,String hash,boolean enabled,java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> roles) {
            super(email,hash,enabled,true,true,true,roles); this.authenticatedHash=hash;
        }
        public String authenticatedHash() { return authenticatedHash; }
    }
    private record Credentials(String email, String password, String status) { }
}
