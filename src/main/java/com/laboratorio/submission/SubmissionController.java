package com.laboratorio.submission;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class SubmissionController {
    private final SubmissionService service;
    public SubmissionController(SubmissionService service) { this.service=service; }
    @GetMapping("/mis-misiones/{id}/entregas")
    public String show(Principal p,Model m,@PathVariable UUID id,@RequestParam(required=false) UUID etapa) { m.addAttribute("id",id); m.addAttribute("vista",service.view(p.getName(),id,etapa)); return "mis-entregas"; }
    @PostMapping("/mis-misiones/{id}/entregas")
    public String send(Principal p,Model m,@PathVariable UUID id,@RequestParam(defaultValue="") String contenido,@RequestParam(required=false) UUID etapa,@RequestParam(required=false) org.springframework.web.multipart.MultipartFile adjunto) {
        try { service.send(p.getName(),id,etapa,contenido,adjunto); return "redirect:/mis-misiones/"+id+"/entregas?guardado"+(etapa == null ? "" : "&etapa="+etapa); }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return show(p,m,id,etapa); }
    }
}
