package com.laboratorio.chat;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class ChatController {
    private final ChatService chat;
    public ChatController(ChatService chat) { this.chat=chat; }
    @GetMapping("/chat")
    public String index(Principal principal,Model model) { model.addAttribute("vista",chat.view(principal.getName())); return "chat"; }
    @PostMapping("/chat/tripulacion")
    public String crew(Principal principal,@RequestParam UUID tripulacion) { return "redirect:/chat/"+chat.crewRoom(principal.getName(),tripulacion); }
    @PostMapping("/chat/privado")
    public String privateRoom(Principal principal,@RequestParam UUID usuario) { return "redirect:/chat/"+chat.privateRoom(principal.getName(),usuario); }
    @GetMapping("/chat/{id}")
    public String room(Principal principal,Model model,@PathVariable UUID id,@RequestParam(defaultValue="0") int pagina) { model.addAttribute("sala",chat.room(principal.getName(),id)); model.addAttribute("pagina",chat.messagesPage(principal.getName(),id,pagina)); return "chat-sala"; }
}
