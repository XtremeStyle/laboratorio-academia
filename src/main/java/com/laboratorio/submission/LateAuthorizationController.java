package com.laboratorio.submission;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class LateAuthorizationController {
    private final LateAuthorizationService service;
    public LateAuthorizationController(LateAuthorizationService service) { this.service=service; }
    @GetMapping("/academia/autorizaciones")
    public String index(Principal p,Model m) { m.addAttribute("opciones",service.options(p.getName())); m.addAttribute("autorizaciones",service.permissions(p.getName())); return "academia-autorizaciones"; }
    @PostMapping("/academia/autorizaciones")
    public String create(Principal p,Model m,@RequestParam String asignacion,@RequestParam(defaultValue="") String fecha,@RequestParam(defaultValue="") String motivo) {
        try { String[] parts=asignacion.split(":",-1); if(parts.length<1 || parts.length>2) throw new IllegalArgumentException("Selecciona una etapa válida."); service.authorize(p.getName(),UUID.fromString(parts[0]),parts.length==2 ? UUID.fromString(parts[1]) : null,fecha,motivo); return "redirect:/academia/autorizaciones?guardado"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
    @PostMapping("/academia/autorizaciones/revocar")
    public String revoke(Principal p,Model m,@RequestParam UUID autorizacion) {
        try { service.revoke(p.getName(),autorizacion); return "redirect:/academia/autorizaciones?revocada"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
}
