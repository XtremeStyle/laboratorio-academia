package com.laboratorio.identity;

import java.security.Principal;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserAdministrationController {
    private final UserAdministration users;
    public UserAdministrationController(UserAdministration users) { this.users = users; }
    @GetMapping("/administracion/usuarios")
    public String list(Principal actor, Model model) {
        model.addAttribute("usuarios", users.listar(actor.getName()));
        return "admin-usuarios";
    }
    @PostMapping("/administracion/usuarios/roles")
    public String roles(Principal actor,Model model,@RequestParam java.util.UUID usuario,@RequestParam(name="roles",required=false) List<String> roles) {
        try { users.cambiarRoles(actor.getName(),usuario,roles); return "redirect:/administracion/usuarios?actualizado"; }
        catch(IllegalArgumentException ex) { model.addAttribute("error",ex.getMessage()); return list(actor,model); }
    }
    @PostMapping("/administracion/usuarios/estado")
    public String state(Principal actor,Model model,@RequestParam java.util.UUID usuario,@RequestParam String estado) {
        try { users.cambiarEstado(actor.getName(),usuario,estado); return "redirect:/administracion/usuarios?actualizado"; }
        catch(IllegalArgumentException ex) { model.addAttribute("error",ex.getMessage()); return list(actor,model); }
    }
    @PostMapping("/administracion/usuarios/nombre")
    public String editName(Principal actor,Model model,@RequestParam java.util.UUID usuario,@RequestParam(defaultValue="") String nombre,@RequestParam(defaultValue="") String apellido) {
        try { users.editarNombre(actor.getName(),usuario,nombre,apellido); return "redirect:/administracion/usuarios?actualizado"; }
        catch(IllegalArgumentException ex) { model.addAttribute("error",ex.getMessage()); return list(actor,model); }
    }
    @PostMapping("/administracion/usuarios")
    public String create(Principal actor, Model model,
            @RequestParam(defaultValue = "") String correo,
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String apellido,
            @RequestParam(defaultValue = "") String contrasena,
            @RequestParam(name = "roles", required = false) List<String> roles) {
        try {
            users.crear(actor.getName(), correo, nombre, apellido, contrasena, roles);
            return "redirect:/administracion/usuarios?creado";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DuplicateKeyException ex) {
            model.addAttribute("error", "Este correo ya está registrado.");
        }
        model.addAttribute("usuarios", users.listar(actor.getName()));
        return "admin-usuarios";
    }
}
