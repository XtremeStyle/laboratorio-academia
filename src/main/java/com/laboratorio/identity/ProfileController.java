package com.laboratorio.identity;
import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service=service; }
    @GetMapping("/mi-perfil")
    public String index(Principal p,Model m) { m.addAttribute("perfil",service.view(p.getName())); return "mi-perfil"; }
    @PostMapping("/mi-perfil")
    public String update(Principal p,Model m,@RequestParam(defaultValue="") String nombre,@RequestParam(defaultValue="") String apellido) {
        try { service.update(p.getName(),nombre,apellido); return "redirect:/mi-perfil?guardado"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(p,m); }
    }
    @PostMapping("/mi-perfil/contrasena")
    public String password(Authentication auth,Model m,HttpServletRequest request,HttpServletResponse response,@RequestParam(defaultValue="") String actual,@RequestParam(defaultValue="") String nueva,@RequestParam(defaultValue="") String confirmacion) {
        try { service.changePassword(auth.getName(),actual,nueva,confirmacion); new SecurityContextLogoutHandler().logout(request,response,auth); return "redirect:/login?contrasenaCambiada"; }
        catch(IllegalArgumentException ex) { m.addAttribute("error",ex.getMessage()); return index(auth,m); }
    }
}
