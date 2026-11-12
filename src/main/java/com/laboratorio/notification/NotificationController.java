package com.laboratorio.notification;

import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service=service; }
    @GetMapping("/notificaciones")
    public String index(Principal principal,Model model) { model.addAttribute("notificaciones",service.list(principal.getName())); return "notificaciones"; }
    @PostMapping("/notificaciones/leida")
    public String read(Principal principal,@RequestParam UUID id) { service.markRead(principal.getName(),id); return "redirect:/notificaciones"; }
}
