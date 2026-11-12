package com.laboratorio.lab;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AstronomyController {
    @GetMapping("/laboratorio/astronomia")
    public String astronomy() {
        return "laboratorio-astronomia";
    }
}
