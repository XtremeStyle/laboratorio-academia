package com.laboratorio.evaluation;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class EvaluationController {
    private final EvaluationService service;
    public EvaluationController(EvaluationService service) { this.service=service; }
    @GetMapping("/academia/evaluaciones")
    public String index(Principal p,Model m) { m.addAttribute("entregas",service.deliveries(p.getName())); return "academia-evaluaciones"; }
    @PostMapping("/academia/evaluaciones")
    public String grade(Principal p,Model m,@RequestParam UUID entrega,@RequestParam(defaultValue="") String nota,@RequestParam(defaultValue="") String observacion,@RequestParam java.util.Map<String,String> parameters) {
        try { service.grade(p.getName(),entrega,nota,observacion,parameters); return "redirect:/academia/evaluaciones?guardado"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
}
