package com.laboratorio.crew;
import java.security.Principal;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CrewController {
    private final CrewService service;
    public CrewController(CrewService service) { this.service=service; }
    @GetMapping("/administracion/tripulaciones")
    public String index(Principal p,Model m) { m.addAttribute("vista",service.view(p.getName())); return "admin-tripulaciones"; }
    @PostMapping("/administracion/tripulaciones")
    public String create(Principal p,Model m,@RequestParam UUID semillero,@RequestParam(defaultValue="") String nombre) {
        try { service.create(p.getName(),semillero,nombre); return "redirect:/administracion/tripulaciones?guardado"; }
        catch (IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
        catch (DuplicateKeyException ex) { m.addAttribute("error","Ya existe una Tripulación con ese nombre en el semillero."); return index(p,m); }
    }
    @PostMapping("/administracion/tripulaciones/miembros")
    public String add(Principal p,Model m,@RequestParam UUID tripulacion,@RequestParam UUID usuario,@RequestParam String funcion) {
        try { service.add(p.getName(),tripulacion,usuario,funcion); return "redirect:/administracion/tripulaciones?guardado"; }
        catch (IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
    @PostMapping("/administracion/tripulaciones/miembros/cerrar")
    public String close(Principal p,Model m,@RequestParam UUID membresia) {
        try { service.close(p.getName(),membresia); return "redirect:/administracion/tripulaciones?guardado"; }
        catch (IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
}
