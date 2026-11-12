package com.laboratorio.chat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatIntegrationTest {
    @Autowired JdbcTemplate db; @Autowired ChatService chat; @Autowired MockMvc mvc;
    UUID academy=UUID.randomUUID(),track=UUID.randomUUID(),student=UUID.randomUUID(),teacher=UUID.randomUUID(),other=UUID.randomUUID(),crew=UUID.randomUUID(),teacherAssignment;
    @BeforeEach void setup() {
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'Chat','ACTIVA')",academy);
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'CHAT','Chat','ACTIVO')",track,academy);
        person(student,"chat-student@example.test","ESTUDIANTE"); person(teacher,"chat-teacher@example.test","PROFESOR"); person(other,"chat-other@example.test","ESTUDIANTE");
        assign(student); teacherAssignment=assign(teacher); assign(other);
        db.update("INSERT INTO tripulacion(id,academia_id,semillero_id,nombre,estado) VALUES (?,?,?,'Nova','ACTIVA')",crew,academy,track);
        UUID membership=UUID.randomUUID(),period=UUID.randomUUID();
        db.update("INSERT INTO membresia_tripulacion(id,academia_id,tripulacion_id,usuario_id) VALUES (?,?,?,?)",membership,academy,crew,student);
        db.update("INSERT INTO periodo_membresia(id,membresia_id,inicio,funcion,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,'PARTICIPANTE',?)",period,membership,teacher);
        db.update("INSERT INTO membresia_activa(membresia_id,periodo_id) VALUES (?,?)",membership,period);
    }
    void person(UUID id,String email,String role) {
        db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Nombre',?,'hash','ACTIVO')",id,academy,email,role);
        db.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo=?",UUID.randomUUID(),id,role);
    }
    UUID assign(UUID user) {
        UUID assignment=UUID.randomUUID(),period=UUID.randomUUID();
        db.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)",assignment,academy,user,track);
        db.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)",period,assignment,user);
        db.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period); return assignment;
    }
    @Test void duplicateMessageIsRejectedWithinTenSeconds() throws Exception {
        UUID channel=chat.crewRoom("chat-student@example.test",crew);
        chat.send("chat-student@example.test",channel,"repetido");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->chat.send("chat-student@example.test",channel,"repetido"));
    }

    @Test void conversationsEnforceMembershipAndRenderTextSafely() throws Exception {
        UUID channel=chat.crewRoom("chat-student@example.test",crew);
        assertEquals(channel,chat.crewRoom("chat-teacher@example.test",crew));
        assertThrows(ResponseStatusException.class,()->chat.room("chat-other@example.test",channel));
        UUID privateRoom=chat.privateRoom("chat-student@example.test",teacher);
        assertEquals(2,chat.view("chat-student@example.test").rooms().size());
        assertEquals(1,chat.view("chat-student@example.test").peers().size());
        assertEquals(privateRoom,chat.privateRoom("chat-teacher@example.test",student));
        assertThrows(ResponseStatusException.class,()->chat.privateRoom("chat-student@example.test",other));
        chat.send("chat-student@example.test",privateRoom,"<script>mensaje</script>");
        assertEquals(1,chat.messages("chat-teacher@example.test",privateRoom).size());
        for(int index=0;index<100;index++) chat.send("chat-student@example.test",privateRoom,"mensaje "+index);
        assertEquals(100,chat.messagesPage("chat-teacher@example.test",privateRoom,0).items().size());
        assertTrue(chat.messagesPage("chat-teacher@example.test",privateRoom,0).more());
        assertEquals(1,chat.messagesPage("chat-teacher@example.test",privateRoom,1).items().size());
        mvc.perform(get("/chat/"+privateRoom).param("pagina","1").with(user("chat-teacher@example.test").roles("PROFESOR"))).andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;mensaje&lt;/script&gt;")));
        mvc.perform(get("/chat/"+privateRoom).with(user("chat-other@example.test").roles("ESTUDIANTE"))).andExpect(status().isForbidden());
        db.update("DELETE FROM asignacion_activa WHERE asignacion_id=?",teacherAssignment);
        assertThrows(ResponseStatusException.class,()->chat.room("chat-teacher@example.test",channel));
        assertThrows(ResponseStatusException.class,()->chat.messages("chat-teacher@example.test",privateRoom));
        assertTrue(chat.view("chat-teacher@example.test").rooms().isEmpty());
    }
}
