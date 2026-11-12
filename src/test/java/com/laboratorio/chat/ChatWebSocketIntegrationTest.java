package com.laboratorio.chat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.datasource.url=jdbc:h2:mem:chat_socket_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class ChatWebSocketIntegrationTest {
    @LocalServerPort int port; @Autowired JdbcTemplate db; @Autowired PasswordEncoder passwords; @Autowired ChatService chat;
    UUID academy=UUID.randomUUID(),track=UUID.randomUUID(),student=UUID.randomUUID(),teacher=UUID.randomUUID();
    @BeforeEach void setup() {
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'Sockets','ACTIVA')",academy);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'SOCKET','Socket','ACTIVO')",track,academy);
        person(student,"socket-student@example.test","ESTUDIANTE"); person(teacher,"socket-teacher@example.test","PROFESOR");
    }
    void person(UUID id,String email,String role) {
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Usuario',?,?,'ACTIVO')",id,academy,email,role,passwords.encode("clave-de-prueba"));
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo=?",UUID.randomUUID(),id,role);
        UUID assignment=UUID.randomUUID(),period=UUID.randomUUID();
        db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)",assignment,academy,id,track);
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)",period,assignment,id);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period);
    }
    HttpClient login(String email) throws Exception {
        var cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL); var client=HttpClient.newBuilder().cookieHandler(cookies).build();
        String base="http://localhost:"+port;
        String page=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString()).body();
        var match=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page); assertTrue(match.find());
        String body="username="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password=clave-de-prueba&_csrf="+URLEncoder.encode(match.group(1),StandardCharsets.UTF_8);
        var result=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(302,result.statusCode()); assertFalse(result.headers().firstValue("Location").orElse("").contains("error")); return client;
    }
    @Test void authenticatedSocketsDeliverAndRecheckPermissions() throws Exception {
        UUID room=chat.privateRoom("socket-student@example.test",teacher);
        HttpClient one=login("socket-student@example.test"),two=login("socket-teacher@example.test");
        var first=new Listener(); var second=new Listener(); String base="http://localhost:"+port; URI uri=URI.create("ws://localhost:"+port+"/chat/socket/"+room);
        var rejected=assertThrows(java.util.concurrent.ExecutionException.class,()->one.newWebSocketBuilder().header("Origin","https://ajeno.example").buildAsync(uri,new Listener()).get(10,TimeUnit.SECONDS));
        assertInstanceOf(java.net.http.WebSocketHandshakeException.class,rejected.getCause());
        WebSocket a=one.newWebSocketBuilder().header("Origin",base).buildAsync(uri,first).get(10,TimeUnit.SECONDS);
        WebSocket b=two.newWebSocketBuilder().header("Origin",base).buildAsync(uri,second).get(10,TimeUnit.SECONDS);
        assertTrue(first.messages.poll(10,TimeUnit.SECONDS).contains("history")); assertTrue(second.messages.poll(10,TimeUnit.SECONDS).contains("history"));
        a.sendText("Hola docente",true).get(5,TimeUnit.SECONDS);
        assertTrue(first.messages.poll(10,TimeUnit.SECONDS).contains("Hola docente")); assertTrue(second.messages.poll(10,TimeUnit.SECONDS).contains("Hola docente"));
        String chatPage=two.send(HttpRequest.newBuilder(URI.create(base+"/chat")).GET().build(),HttpResponse.BodyHandlers.ofString()).body();
        var csrf=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(chatPage); assertTrue(csrf.find());
        var logout=two.send(HttpRequest.newBuilder(URI.create(base+"/logout")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("_csrf="+URLEncoder.encode(csrf.group(1),StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString()); assertEquals(302,logout.statusCode());
        a.sendText("Segundo mensaje",true).get(5,TimeUnit.SECONDS);
        assertTrue(first.messages.poll(10,TimeUnit.SECONDS).contains("Segundo mensaje"));
        assertEquals(1008,second.closed.get(10,TimeUnit.SECONDS));
        db.update("UPDATE usuario SET estado='INACTIVO' WHERE id=?",teacher);
        a.sendText("Tercer mensaje",true).get(5,TimeUnit.SECONDS);
        assertEquals(1008,first.closed.get(10,TimeUnit.SECONDS));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM mensaje WHERE conversacion_id=?",Integer.class,room));
        a.abort(); b.abort();
    }
    static class Listener implements WebSocket.Listener {
        final LinkedBlockingQueue<String> messages=new LinkedBlockingQueue<>(); final CompletableFuture<Integer> closed=new CompletableFuture<>(); final StringBuilder buffer=new StringBuilder();
        @Override public void onOpen(WebSocket socket) { socket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last) { buffer.append(data); if(last) { messages.add(buffer.toString()); buffer.setLength(0); } socket.request(1); return null; }
        @Override public CompletionStage<?> onClose(WebSocket socket,int code,String reason) { closed.complete(code); return null; }
        @Override public void onError(WebSocket socket,Throwable error) { closed.completeExceptionally(error); }
    }
}
