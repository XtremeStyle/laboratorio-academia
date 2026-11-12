package com.laboratorio.progress;
import java.security.Principal;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class CompetencyController {
    private final CompetencyService service;
    private final ProgressService progress;
    public CompetencyController(CompetencyService service,ProgressService progress) { this.service=service; this.progress=progress; }
    @GetMapping("/academia/progreso")
    public String report(Principal p,Model m) { m.addAttribute("progreso",progress.report(p.getName())); return "academia-progreso"; }
    @GetMapping("/academia/competencias")
    public String index(Principal p,Model m) { m.addAttribute("vista",service.view(p.getName())); return "academia-competencias"; }
    @PostMapping("/academia/competencias")
    public String create(Principal p,Model m,@RequestParam UUID semillero,@RequestParam(defaultValue="") String codigo,@RequestParam(defaultValue="") String nombre) {
        try { service.create(p.getName(),semillero,codigo,nombre); return "redirect:/academia/competencias?guardado"; }
        catch(DuplicateKeyException ex) { m.addAttribute("error","Ya existe una competencia con ese código."); return index(p,m); }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
    @PostMapping("/academia/competencias/vincular")
    public String link(Principal p,Model m,@RequestParam UUID criterio,@RequestParam UUID competencia) {
        try { service.link(p.getName(),criterio,competencia); return "redirect:/academia/competencias?vinculada"; }
        catch(DuplicateKeyException ex) { m.addAttribute("error","La competencia ya está vinculada a ese criterio."); return index(p,m); }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
}
