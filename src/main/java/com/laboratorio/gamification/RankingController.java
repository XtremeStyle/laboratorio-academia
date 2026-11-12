package com.laboratorio.gamification;

import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RankingController {
    private final RankingService service;
    public RankingController(RankingService service) { this.service=service; }
    @GetMapping("/administracion/clasificacion")
    public String global(Principal principal,Model model) { model.addAttribute("clasificacion",service.global(principal.getName())); return "clasificacion-global"; }
    @GetMapping("/clasificacion")
    public String index(Principal principal,Model model,@RequestParam(required=false) UUID semillero) {
        model.addAttribute("semilleros",service.tracks(principal.getName()));
        if(semillero!=null) model.addAttribute("ranking",service.ranking(principal.getName(),semillero));
        return "clasificacion";
    }
}
