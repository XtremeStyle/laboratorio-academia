package com.laboratorio.identity;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

/** Comprueba sesiones creadas por el inicio de sesión local en cada petición. */
public class SessionAccountFilter extends OncePerRequestFilter {
    public static final String ACCOUNT_SNAPSHOT="LAB_ACCOUNT_SNAPSHOT";
    private final JdbcTemplate db;
    public SessionAccountFilter(JdbcTemplate db) { this.db=db; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var session=request.getSession(false);
        Object snapshot=session==null ? null : session.getAttribute(ACCOUNT_SNAPSHOT);
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(snapshot instanceof String hash && authentication!=null && authentication.getPrincipal() instanceof UserDetails principal) {
            var accounts=db.query("SELECT password_hash,estado FROM usuario WHERE correo=?",(rs,i)->new Account(rs.getString(1),rs.getString(2)),principal.getUsername());
            boolean valid=accounts.size()==1 && "ACTIVO".equals(accounts.getFirst().status()) && accounts.getFirst().hash().equals(hash);
            if(valid) {
                Set<String> roles=new HashSet<>(db.query("SELECT 'ROLE_' || r.codigo FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=?",(rs,i)->rs.getString(1),principal.getUsername()));
                Set<String> current=new HashSet<>(); principal.getAuthorities().forEach(a->current.add(a.getAuthority()));
                valid=roles.equals(current);
            }
            if(!valid) { SecurityContextHolder.clearContext(); session.invalidate(); }
        }
        chain.doFilter(request,response);
    }
    private record Account(String hash,String status) {}
}
