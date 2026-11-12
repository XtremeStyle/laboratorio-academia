package com.laboratorio.course;

import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CourseController {
    private final CourseService courses;
    public CourseController(CourseService courses) { this.courses = courses; }

    @GetMapping("/cursos")
    public String catalog(Principal principal, Model model) {
        CourseService.CatalogView vista = courses.catalogFor(principal.getName());
        model.addAttribute("cursos", vista.courses());
        model.addAttribute("esEstudiante", vista.student());
        model.addAttribute("cursosDocente", vista.staffCourses());
        return "cursos";
    }

    @PostMapping("/cursos/matricular")
    public String enroll(Principal principal, @RequestParam UUID curso, @RequestParam UUID semillero) {
        courses.enroll(principal.getName(), curso, semillero);
        return "redirect:/cursos?matriculado";
    }

    @GetMapping("/cursos/estudiar")
    public String detail(Principal principal, @RequestParam UUID matricula, Model model) {
        model.addAttribute("detalle", courses.detail(principal.getName(), matricula));
        return "curso-detalle";
    }

    @GetMapping("/academia/cursos/progreso")
    public String staffProgress(Principal principal, @RequestParam UUID curso, @RequestParam UUID semillero,
            @RequestParam(defaultValue="0") int pagina, Model model) {
        var result=courses.staffProgress(principal.getName(),curso,semillero,pagina);
        model.addAttribute("progreso",result.rows());
        model.addAttribute("pagina",result.page());
        model.addAttribute("haySiguiente",result.hasNext());
        model.addAttribute("curso",curso);
        model.addAttribute("semillero",semillero);
        return "academia-cursos-progreso";
    }

    @PostMapping("/cursos/leccion")
    public String updateLesson(Principal principal, @RequestParam UUID matricula,
            @RequestParam UUID leccion, @RequestParam String estado,
            @RequestParam(defaultValue = "") String evidencia, RedirectAttributes flash) {
        try {
            courses.updateLesson(principal.getName(), matricula, leccion, estado, evidencia);
            flash.addFlashAttribute("mensajeLeccion", "Tu avance se guardó correctamente.");
            flash.addFlashAttribute("leccionActualizada", leccion.toString());
        } catch (IllegalArgumentException invalid) {
            flash.addFlashAttribute("errorLeccion", invalid.getMessage());
            flash.addFlashAttribute("leccionConError", leccion.toString());
            flash.addFlashAttribute("evidenciaIngresada", evidencia.length() <= 5000 ? evidencia : evidencia.substring(0, 5000));
        }
        return "redirect:/cursos/estudiar?matricula=" + matricula + "#leccion-" + leccion;
    }
}
