package com.laboratorio.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.identity.DatabaseUserDetailsService.SessionUser;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatSocketHandler extends TextWebSocketHandler {
    private final ChatService chat; private final JdbcTemplate db; private final ObjectMapper json;
    private final Map<UUID,Set<WebSocketSession>> rooms=new ConcurrentHashMap<>();
    private final Object[] locks=new Object[64];
    public ChatSocketHandler(ChatService chat,JdbcTemplate db,ObjectMapper json) { this.chat=chat; this.db=db; this.json=json; java.util.Arrays.setAll(locks,i->new Object()); }
    private Object lock(UUID id) { return locks[(id.hashCode() & Integer.MAX_VALUE)%locks.length]; }
    private UUID room(WebSocketSession session) { String path=session.getUri().getPath(); return UUID.fromString(path.substring(path.lastIndexOf('/')+1)); }
    private boolean current(WebSocketSession socket) {
        Principal principal=socket.getPrincipal();
        if(!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof SessionUser user)) return false;
        Object stored=socket.getAttributes().get("LAB_HTTP_SESSION");
        if(!(stored instanceof jakarta.servlet.http.HttpSession httpSession)) return false;
        try { if(!user.authenticatedHash().equals(httpSession.getAttribute(com.laboratorio.identity.SessionAccountFilter.ACCOUNT_SNAPSHOT))) return false; }
        catch(IllegalStateException ex) { return false; }
        var hashes=db.query("SELECT password_hash FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->rs.getString(1),user.getUsername());
        if(hashes.size()!=1 || !hashes.getFirst().equals(user.authenticatedHash())) return false;
        Set<String> actual=new HashSet<>(db.query("SELECT 'ROLE_' || r.codigo FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=?",(rs,i)->rs.getString(1),user.getUsername()));
        Set<String> original=new HashSet<>(); user.getAuthorities().forEach(a->original.add(a.getAuthority())); return actual.equals(original);
    }
    private boolean allowed(WebSocketSession session,UUID id) {
        Principal principal=session.getPrincipal();
        if(!current(session)) return false;
        try { chat.room(principal.getName(),id); return true; } catch(org.springframework.web.server.ResponseStatusException ex) { return false; }
    }
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID id;
        try { id=room(session); } catch(RuntimeException ex) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        if(!allowed(session,id)) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        synchronized(lock(id)) {
            rooms.computeIfAbsent(id,k->ConcurrentHashMap.newKeySet()).add(session);
            session.sendMessage(new TextMessage(json.writeValueAsString(Map.of("type","history","messages",chat.messages(session.getPrincipal().getName(),id)))));
        }
    }
    @Override protected void handleTextMessage(WebSocketSession session,TextMessage message) throws Exception {
        UUID id=room(session);
        synchronized(lock(id)) {
            if(!allowed(session,id)) { session.close(CloseStatus.POLICY_VIOLATION); return; }
            ChatService.Message saved;
            try { saved=chat.send(session.getPrincipal().getName(),id,message.getPayload()); }
            catch(IllegalArgumentException ex) { session.sendMessage(new TextMessage(json.writeValueAsString(Map.of("type","error","message",ex.getMessage())))); return; }
            String payload=json.writeValueAsString(Map.of("type","message","message",saved));
            for(WebSocketSession receiver:rooms.getOrDefault(id,Set.of())) {
                try {
                    if(receiver.isOpen() && allowed(receiver,id)) receiver.sendMessage(new TextMessage(payload));
                    else if(receiver.isOpen()) receiver.close(CloseStatus.POLICY_VIOLATION);
                } catch(IOException ex) { try { receiver.close(CloseStatus.SERVER_ERROR); } catch(IOException ignored) { } }
            }
        }
    }
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status) {
        try { UUID id=room(session); var set=rooms.get(id); if(set!=null) { set.remove(session); if(set.isEmpty()) rooms.remove(id,set); } } catch(RuntimeException ignored) { }
    }
}
