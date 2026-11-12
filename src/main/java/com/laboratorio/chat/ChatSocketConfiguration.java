package com.laboratorio.chat;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
@Configuration
@EnableWebSocket
public class ChatSocketConfiguration implements WebSocketConfigurer {
    private final ChatSocketHandler handler;
    public ChatSocketConfiguration(ChatSocketHandler handler) { this.handler=handler; }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) { registry.addHandler(handler,"/chat/socket/*").addInterceptors(new org.springframework.web.socket.server.HandshakeInterceptor() {
        @Override public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,org.springframework.http.server.ServerHttpResponse response,org.springframework.web.socket.WebSocketHandler handler,java.util.Map<String,Object> attributes) {
            if(!(request instanceof org.springframework.http.server.ServletServerHttpRequest servlet)) return false;
            var session=servlet.getServletRequest().getSession(false); if(session==null) return false;
            attributes.put("LAB_HTTP_SESSION",session); return true;
        }
        @Override public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,org.springframework.http.server.ServerHttpResponse response,org.springframework.web.socket.WebSocketHandler handler,Exception ex) { }
    }); }
}
