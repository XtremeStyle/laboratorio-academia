package com.laboratorio.audit;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AuditController {
    private final AuditService service;
    public AuditController(AuditService service) { this.service=service; }
    @GetMapping("/administracion/auditoria")
    public String index(Principal principal,Model model) { model.addAttribute("eventos",service.list(principal.getName())); return "admin-auditoria"; }
}
