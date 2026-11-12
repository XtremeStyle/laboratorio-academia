package com.laboratorio.report;
import java.io.IOException;
import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) { this.service=service; }
    @GetMapping("/academia/boletines")
    public String list(Principal principal,org.springframework.ui.Model model) { model.addAttribute("estudiantes",service.students(principal.getName())); return "academia-boletines"; }
    @GetMapping("/academia/boletines/{id}.pdf")
    public ResponseEntity<byte[]> student(Principal principal,@org.springframework.web.bind.annotation.PathVariable java.util.UUID id) throws IOException {
        return pdf(service.forStudent(principal.getName(),id));
    }
    @GetMapping("/mi-boletin.pdf")
    public ResponseEntity<byte[]> mine(Principal principal) throws IOException {
        return pdf(service.mine(principal.getName()));
    }
    private ResponseEntity<byte[]> pdf(byte[] pdf) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=boletin-laboratorio.pdf").header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
