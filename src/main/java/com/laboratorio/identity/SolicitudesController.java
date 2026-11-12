package com.laboratorio.identity;

import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Bandeja administrativa de solicitudes de acceso: aprobar genera la contraseña temporal (se muestra una sola vez). */
@Controller
public class SolicitudesController {
    private final SolicitudService solicitudes;

    public SolicitudesController(SolicitudService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @GetMapping("/administracion/solicitudes")
    public String bandeja(Principal principal, Model model) {
        model.addAttribute("solicitudes", solicitudes.listar(principal.getName()));
        return "admin-solicitudes";
    }

    @PostMapping("/administracion/solicitudes/aprobar")
    public String aprobar(Principal principal, Model model, @RequestParam UUID solicitud) {
        try {
            SolicitudService.Aprobacion aprobada = solicitudes.aprobar(principal.getName(), solicitud);
            model.addAttribute("aprobada", aprobada);
            return "admin-solicitud-aprobada";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("solicitudes", solicitudes.listar(principal.getName()));
            return "admin-solicitudes";
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            model.addAttribute("error", "La solicitud no existe o ya fue resuelta.");
            model.addAttribute("solicitudes", solicitudes.listar(principal.getName()));
            return "admin-solicitudes";
        }
    }

    @PostMapping("/administracion/solicitudes/rechazar")
    public String rechazar(Principal principal, @RequestParam UUID solicitud) {
        try {
            solicitudes.rechazar(principal.getName(), solicitud);
            return "redirect:/administracion/solicitudes?rechazada";
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            return "redirect:/administracion/solicitudes?error";
        }
    }
}
