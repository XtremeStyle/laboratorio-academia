package com.laboratorio.research;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Profile("dev")
public class DemoSemillerosController {
    private final DemoSemilleros semilleros;

    public DemoSemillerosController(DemoSemilleros semilleros) { this.semilleros = semilleros; }

    @GetMapping("/demostracion/semilleros")
    public String consultar(@RequestParam(defaultValue = "") String q, Model model) {
        String busqueda = q.strip();
        model.addAttribute("q", busqueda);
        if (busqueda.length() > 100) {
            model.addAttribute("error", "La búsqueda admite hasta 100 caracteres.");
            model.addAttribute("semilleros", java.util.List.of());
        } else {
            model.addAttribute("semilleros", semilleros.buscar(busqueda));
        }
        return "semilleros-demo";
    }
}
