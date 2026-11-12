package com.laboratorio.mission;

import java.security.Principal;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MissionController {
    private final MissionService service;
    public MissionController(MissionService service) { this.service=service; }
    @GetMapping("/academia/misiones")
    public String index(Principal p,Model m) { m.addAttribute("vista",service.view(p.getName())); return "academia-misiones"; }
    @PostMapping("/academia/misiones")
    public String create(Principal p,Model m,@RequestParam UUID semillero,@RequestParam(defaultValue="") String titulo,
            @RequestParam(defaultValue="") String instrucciones,@RequestParam(defaultValue="") String criterio,
            @RequestParam(defaultValue="") String modalidad,@RequestParam(defaultValue="") String fecha,@RequestParam(required=false) java.util.List<UUID> adicionales) {
        try { service.create(p.getName(),semillero,titulo,instrucciones,criterio,modalidad,fecha,adicionales); return "redirect:/academia/misiones?guardado"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
    @PostMapping("/academia/misiones/etapas")
    public String stage(Principal p,Model m,@RequestParam UUID mision,@RequestParam(defaultValue="") String titulo,
            @RequestParam(defaultValue="") String fecha,@RequestParam(defaultValue="false") boolean obligatoria,@RequestParam(defaultValue="") String criterio) {
        try { service.addStage(p.getName(),mision,titulo,fecha,obligatoria,criterio); return "redirect:/academia/misiones?etapa"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
    @PostMapping("/academia/misiones/asignar")
    public String assign(Principal p,Model m,@RequestParam String mision,@RequestParam UUID destinatario,@RequestParam(defaultValue="") String tipo) {
        try { String[] ids=mision.split(":",-1); if(ids.length<1 || ids.length>2) throw new IllegalArgumentException("Selecciona una Misión válida."); service.assign(p.getName(),UUID.fromString(ids[0]),destinatario,ids.length==2 ? UUID.fromString(ids[1]) : null,tipo); return "redirect:/academia/misiones?asignado"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
        catch(DuplicateKeyException ex) { m.addAttribute("error","La Misión ya está asignada a ese destinatario."); return index(p,m); }
    }
    @GetMapping("/academia/cierres")
    public String completions(Principal p,Model m) { m.addAttribute("opciones",service.completionOptions(p.getName())); return "academia-cierres"; }
    @PostMapping("/academia/cierres")
    public String complete(Principal p,Model m,@RequestParam UUID asignacion) {
        try { service.complete(p.getName(),asignacion); return "redirect:/academia/cierres?cerrada"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return completions(p,m); }
    }
    @GetMapping("/mis-misiones")
    public String mine(Principal p,Model m,@RequestParam(defaultValue="0") int pagina) { var result=service.minePage(p.getName(),pagina); m.addAttribute("misiones",result.items()); m.addAttribute("pagina",result); return "mis-misiones"; }
}
