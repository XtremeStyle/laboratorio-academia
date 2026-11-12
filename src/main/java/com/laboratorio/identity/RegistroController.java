package com.laboratorio.identity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Registro público "Solicitar acceso": formulario + confirmación neutra. Nunca revela qué correos existen. */
@Controller
public class RegistroController {
    private final SolicitudService solicitudes;

    public RegistroController(SolicitudService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @GetMapping("/registro")
    public String registro() { return "registro"; }

    @PostMapping("/registro")
    public String crear(Model model, @RequestParam String nombre, @RequestParam String apellido,
                        @RequestParam String correo, @RequestParam String rol, @RequestParam(required = false) String mensaje) {
        try {
            SolicitudService.ResultadoSolicitud resultado = solicitudes.solicitar(correo, nombre, apellido, rol, mensaje);
            model.addAttribute("enviada", true);
            model.addAttribute("sinAcademia", resultado == SolicitudService.ResultadoSolicitud.SIN_ACADEMIA);
            return "registro";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("apellido", apellido);
            model.addAttribute("correo", correo);
            model.addAttribute("rol", rol);
            return "registro";
        } catch (RuntimeException ex) {
            model.addAttribute("error", "No pudimos registrar tu solicitud. Intenta de nuevo en unos minutos.");
            return "registro";
        }
    }
}
