package com.laboratorio.research;

import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AssignmentsController {
    private final Assignments service;
    public AssignmentsController(Assignments service) { this.service = service; }
    @GetMapping("/administracion/asignaciones")
    public String index(Principal principal, Model model) {
        model.addAttribute("vista", service.view(principal.getName()));
        return "admin-asignaciones";
    }
    @PostMapping("/administracion/asignaciones")
    public String assign(Principal principal, Model model, @RequestParam UUID usuario, @RequestParam UUID semillero) {
        try { service.assign(principal.getName(), usuario, semillero); return "redirect:/administracion/asignaciones?guardado"; }
        catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); return index(principal, model); }
    }
    @PostMapping("/administracion/asignaciones/cerrar")
    public String close(Principal principal, Model model, @RequestParam UUID asignacion) {
        try { service.close(principal.getName(), asignacion); return "redirect:/administracion/asignaciones?cerrado"; }
        catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); return index(principal, model); }
    }
}
