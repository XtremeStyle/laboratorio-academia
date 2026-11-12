package com.laboratorio.progress;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class ProgressController {
    private final com.laboratorio.gamification.XpMovementService xp;
    private final ProgressService service;
    private final PerformanceAlertService alerts;
    public ProgressController(ProgressService service,PerformanceAlertService alerts,com.laboratorio.gamification.XpMovementService xp) { this.service=service; this.alerts=alerts; this.xp=xp; }
    @GetMapping("/mi-progreso")
    public String index(Principal p,Model m) { alerts.reconcileUser(p.getName()); m.addAttribute("competencias",service.mine(p.getName())); m.addAttribute("movimientos",xp.movementsFor(p.getName(),15)); return "mi-progreso"; }
}
