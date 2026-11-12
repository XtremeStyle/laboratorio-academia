package com.laboratorio.lab;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class CircuitController {
    @GetMapping("/laboratorio/circuitos")
    public String circuit() { return "laboratorio-circuitos"; }
}
