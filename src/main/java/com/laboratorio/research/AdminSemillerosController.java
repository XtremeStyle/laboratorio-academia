package com.laboratorio.research;

import java.security.Principal;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Gestión acotada a la academia del administrador autenticado. */
@Controller
public class AdminSemillerosController {
    private final JdbcTemplate jdbc;
    private final SemilleroAdministration administration;
    public AdminSemillerosController(JdbcTemplate jdbc,SemilleroAdministration administration) { this.jdbc = jdbc; this.administration=administration; }

    @GetMapping("/administracion/semilleros")
    public String listar(Principal principal, Model model) {
        cargar(principal, model);
        return "admin-semilleros";
    }

    @PostMapping("/administracion/semilleros")
    public String crear(@RequestParam(defaultValue = "") String codigo,
                        @RequestParam(defaultValue = "") String nombre,
                        @RequestParam(defaultValue = "") String descripcion,
                        Principal principal, Model model) {
        try { administration.create(principal.getName(),codigo,nombre,descripcion); }
        catch(IllegalArgumentException ex) { model.addAttribute("error",ex.getMessage()); cargar(principal,model); return "admin-semilleros"; }
        catch(DuplicateKeyException ex) { model.addAttribute("error","Ya existe un semillero con ese código en tu academia."); cargar(principal,model); return "admin-semilleros"; }
        return "redirect:/administracion/semilleros?creado";
    }

    private UUID academiaActual(Principal principal) {
        var academias = jdbc.query("SELECT u.academia_id FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'",
                (rs, row) -> rs.getObject(1, UUID.class), principal.getName());
        if (academias.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return academias.getFirst();
    }

    private void cargar(Principal principal, Model model) {
        model.addAttribute("semilleros", jdbc.query("""
            SELECT codigo,nombre,descripcion FROM semillero
            WHERE academia_id=? AND estado='ACTIVO' ORDER BY nombre LIMIT 100
            """, (rs, row) -> new DemoSemilleros.SemilleroVista(rs.getString(1), rs.getString(2), rs.getString(3)), academiaActual(principal)));
    }
}
