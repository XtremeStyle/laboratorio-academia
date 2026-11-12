package com.laboratorio.presentation.web;

import org.springframework.stereotype.Controller;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final Environment environment;
    private final DashboardService dashboard;
    private final com.laboratorio.gamification.RankingService ranking;
    private final com.laboratorio.gamification.BadgeService badges;

    public HomeController(Environment environment,DashboardService dashboard,com.laboratorio.gamification.RankingService ranking,com.laboratorio.gamification.BadgeService badges) { this.environment = environment; this.dashboard=dashboard; this.ranking=ranking; this.badges=badges; }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        model.addAttribute("consultaDemoDisponible", environment.acceptsProfiles(Profiles.of("dev")));
        model.addAttribute("sesionIniciada", authentication != null && authentication.isAuthenticated());
        model.addAttribute("esDocente", authentication != null && authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR") || authority.getAuthority().equals("ROLE_PROFESOR")));
        if(authentication != null && authentication.isAuthenticated()) { model.addAttribute("panel",dashboard.view(authentication.getName())); model.addAttribute("experiencia",ranking.personal(authentication.getName())); model.addAttribute("insignias",badges.mine(authentication.getName())); }
        model.addAttribute("esAdministrador", authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR")));
        return "index";
    }
}
