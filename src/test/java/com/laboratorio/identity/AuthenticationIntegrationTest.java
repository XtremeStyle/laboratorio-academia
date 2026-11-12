package com.laboratorio.identity;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired com.laboratorio.gamification.BadgeService badges;
    UUID user = UUID.randomUUID();
    @BeforeEach void setup() {
        UUID academy = UUID.randomUUID();
        jdbc.update("INSERT INTO academia(id,nombre,estado) VALUES (?, 'Prueba', 'ACTIVA')", academy);
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'prueba@example.test','Ana','Prueba',?,'ACTIVO')", user, academy, encoder.encode("clave-de-prueba"));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ESTUDIANTE'", UUID.randomUUID(), user);
    }
    @Test void logsInWithDatabaseAndRejectsInvalidPasswordOrInactiveUser() throws Exception {
        mvc.perform(formLogin().user("  PRUEBA@example.test  ").password("clave-de-prueba"))
                .andExpect(authenticated().withUsername("prueba@example.test").withRoles("ESTUDIANTE"));
        mvc.perform(formLogin().user("prueba@example.test").password("incorrecta"))
                .andExpect(unauthenticated());
        jdbc.update("UPDATE usuario SET estado='INACTIVO' WHERE id=?", user);
        mvc.perform(formLogin().user("prueba@example.test").password("clave-de-prueba"))
                .andExpect(unauthenticated());
    }
    @Test void administrationRequiresAdminRole() throws Exception {
        mvc.perform(get("/administracion/cualquier-ruta").with(user("alumna").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/administracion/cualquier-ruta").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }
    @Test void adminCanCreateOnlyWithinOwnAcademyAndRevocationTakesEffect() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), user);
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO academia(id,nombre,estado) VALUES (?, 'Otra', 'ACTIVA')", other);
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'OTRO','Semillero ajeno','ACTIVO')", UUID.randomUUID(), other);
        var admin = user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/semilleros").with(admin).param("codigo","ROB").param("nombre","Robótica"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/administracion/semilleros").with(admin).with(csrf()).param("codigo","ROB").param("nombre","Robótica"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE actor_id=? AND accion='CREAR_SEMILLERO'",Integer.class,user));
        mvc.perform(get("/administracion/semilleros").with(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Robótica")))
                .andExpect(content().string(not(containsString("Semillero ajeno"))));
        mvc.perform(post("/administracion/semilleros").with(admin).with(csrf()).param("codigo","ROB").param("nombre","Duplicado"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Ya existe")));
        jdbc.update("DELETE FROM usuario_rol WHERE usuario_id=? AND rol_id=(SELECT id FROM rol WHERE codigo='ADMINISTRADOR')", user);
        mvc.perform(get("/administracion/semilleros").with(admin)).andExpect(status().isForbidden());
    }

    @Test void createsMultiRoleUserWithBcryptAndEnforcesUniquenessAndCsrf() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), user);
        var admin = user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/usuarios").with(admin).param("correo","nuevo@example.test").param("nombre","Nora").param("apellido","Paz").param("contrasena","una-clave-muy-larga").param("roles","ESTUDIANTE","PROFESOR"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/administracion/usuarios").with(admin).with(csrf()).param("correo","NUEVO@example.test").param("nombre","Nora").param("apellido","Paz").param("contrasena","una-clave-muy-larga").param("roles","ESTUDIANTE","PROFESOR"))
                .andExpect(status().is3xxRedirection());
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE correo='nuevo@example.test'", String.class);
        org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("una-clave-muy-larga", hash));
        org.junit.jupiter.api.Assertions.assertNotEquals("una-clave-muy-larga", hash);
        mvc.perform(formLogin().user("nuevo@example.test").password("una-clave-muy-larga"))
                .andExpect(authenticated().withRoles("ESTUDIANTE", "PROFESOR"));
        mvc.perform(post("/administracion/usuarios").with(admin).with(csrf()).param("correo","nuevo@example.test").param("nombre","Nora").param("apellido","Paz").param("contrasena","una-clave-muy-larga").param("roles","ESTUDIANTE"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("correo ya está registrado")));
    }
    @Test void userAdministrationRejectsUnauthorizedAndInvalidRoles() throws Exception {
        var student = user("prueba@example.test").roles("ESTUDIANTE");
        mvc.perform(get("/administracion/usuarios").with(student)).andExpect(status().isForbidden());
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), user);
        var admin = user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/usuarios").with(admin).with(csrf()).param("correo","malo@example.test").param("nombre","A").param("apellido","B").param("contrasena","contraseña-segura").param("roles","SUPERUSUARIO"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Revisa correo")));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo='malo@example.test'", Integer.class));
    }

    @Test void assignmentCanBeClosedAndReactivatedWithHistory() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), user);
        UUID academy = jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?", UUID.class, user);
        UUID track = UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ASTRO','Astronomía','ACTIVO')", track, academy);
        var admin = user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario", user.toString()).param("semillero", track.toString()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario", user.toString()).param("semillero", track.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("ya está activa")));
        UUID assignment = jdbc.queryForObject("SELECT id FROM asignacion_semillero WHERE usuario_id=? AND semillero_id=?", UUID.class, user, track);
        mvc.perform(post("/administracion/asignaciones/cerrar").with(admin).with(csrf()).param("asignacion",assignment.toString()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",user.toString()).param("semillero",track.toString()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM periodo_asignacion WHERE asignacion_id=?", Integer.class, assignment));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM asignacion_activa WHERE asignacion_id=?", Integer.class, assignment));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM periodo_asignacion WHERE asignacion_id=? AND fin IS NOT NULL", Integer.class, assignment));
    }
    @Test void actualSessionIsInvalidatedWhenAccountIsInactive() throws Exception {
        var login=mvc.perform(formLogin().user("prueba@example.test").password("clave-de-prueba")).andExpect(authenticated()).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/mis-misiones").session(session)).andExpect(status().isOk());
        jdbc.update("UPDATE usuario SET estado='INACTIVO' WHERE id=?",user);
        mvc.perform(get("/mis-misiones").session(session)).andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
    }
    @Test void actualSessionIsInvalidatedWhenRolesChange() throws Exception {
        var login=mvc.perform(formLogin().user("prueba@example.test").password("clave-de-prueba")).andExpect(authenticated()).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='PROFESOR'",UUID.randomUUID(),user);
        mvc.perform(get("/mis-misiones").session(session)).andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
    }

    @Test void deactivationInvalidatesSessionAndProtectsLastAdministrator() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/usuarios/estado").with(admin).with(csrf()).param("usuario",user.toString()).param("estado","INACTIVO")).andExpect(status().isOk()).andExpect(content().string(containsString("último administrador")));
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user),other=UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'estudiante@example.test','Eva','Prueba',?,'ACTIVO')",other,academy,encoder.encode("clave-de-prueba"));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ESTUDIANTE'",UUID.randomUUID(),other);
        var login=mvc.perform(formLogin().user("estudiante@example.test").password("clave-de-prueba")).andExpect(authenticated()).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(post("/administracion/usuarios/estado").with(admin).with(csrf()).param("usuario",other.toString()).param("estado","INACTIVO")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mis-misiones").session(session)).andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
        mvc.perform(post("/administracion/usuarios/estado").with(admin).with(csrf()).param("usuario",other.toString()).param("estado","ACTIVO")).andExpect(status().is3xxRedirection());
        mvc.perform(formLogin().user("estudiante@example.test").password("clave-de-prueba")).andExpect(authenticated());
    }

    @Test void roleChangesProtectLastAdminAndInvalidateExistingSession() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/usuarios/roles").with(admin).with(csrf()).param("usuario",user.toString()).param("roles","ESTUDIANTE")).andExpect(status().isOk()).andExpect(content().string(containsString("último administrador")));
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user),other=UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'roles@example.test','Nora','Prueba',?,'ACTIVO')",other,academy,encoder.encode("clave-de-prueba"));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ESTUDIANTE'",UUID.randomUUID(),other);
        var login=mvc.perform(formLogin().user("roles@example.test").password("clave-de-prueba")).andExpect(authenticated()).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(post("/administracion/usuarios/roles").with(admin).with(csrf()).param("usuario",other.toString()).param("roles","ESTUDIANTE","PROFESOR")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mis-misiones").session(session)).andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
        mvc.perform(formLogin().user("roles@example.test").password("clave-de-prueba")).andExpect(authenticated().withRoles("ESTUDIANTE","PROFESOR"));
    }

    @Test void passwordChangeRequiresCurrentPasswordAndRevokesSessions() throws Exception {
        var first=mvc.perform(formLogin().user("prueba@example.test").password("clave-de-prueba")).andExpect(authenticated()).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)first.getRequest().getSession(false);
        var second=mvc.perform(formLogin().user("prueba@example.test").password("clave-de-prueba")).andExpect(authenticated()).andReturn();
        var other=(org.springframework.mock.web.MockHttpSession)second.getRequest().getSession(false);
        mvc.perform(post("/mi-perfil/contrasena").session(session).with(csrf()).param("actual","incorrecta").param("nueva","nueva-clave-segura").param("confirmacion","nueva-clave-segura")).andExpect(status().isOk()).andExpect(content().string(containsString("contraseña actual no es correcta")));
        mvc.perform(post("/mi-perfil/contrasena").session(session).with(csrf()).param("actual","clave-de-prueba").param("nueva","nueva-clave-segura").param("confirmacion","nueva-clave-segura")).andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
        mvc.perform(get("/mis-misiones").session(other)).andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
        mvc.perform(formLogin().user("prueba@example.test").password("clave-de-prueba")).andExpect(unauthenticated());
        mvc.perform(formLogin().user("prueba@example.test").password("nueva-clave-segura")).andExpect(authenticated());
    }

    @Test void administratorsEditNamesOnlyWithinTheirAcademy() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/usuarios/nombre").with(admin).with(csrf()).param("usuario",user.toString()).param("nombre","María").param("apellido","Luna")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/administracion/usuarios").with(admin)).andExpect(status().isOk()).andExpect(content().string(containsString("María")));
        UUID foreignAcademy=UUID.randomUUID(),foreignUser=UUID.randomUUID();
        jdbc.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'Ajena','ACTIVA')",foreignAcademy);
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'otra@example.test','Otro','Usuario','x','ACTIVO')",foreignUser,foreignAcademy);
        mvc.perform(post("/administracion/usuarios/nombre").with(admin).with(csrf()).param("usuario",foreignUser.toString()).param("nombre","No").param("apellido","Cambio")).andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals("Otro",jdbc.queryForObject("SELECT nombre FROM usuario WHERE id=?",String.class,foreignUser));
    }

    @Test void assignmentCannotCrossAcademies() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), user);
        UUID academy = UUID.randomUUID(), foreignTrack = UUID.randomUUID();
        jdbc.update("INSERT INTO academia(id,nombre,estado) VALUES (?,'Otra','ACTIVA')",academy);
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'AJENO','Otro','ACTIVO')",foreignTrack,academy);
        mvc.perform(post("/administracion/asignaciones").with(user("prueba@example.test").roles("ADMINISTRADOR")).with(csrf()).param("usuario",user.toString()).param("semillero",foreignTrack.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("no disponible en tu academia")));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM asignacion_semillero WHERE semillero_id=?",Integer.class,foreignTrack));
    }

    @Test void crewMembershipRequiresTrackAssignmentAndProtectsHistory() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'", UUID.randomUUID(), user);
        UUID academy = jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user), track=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROB','Robótica','ACTIVO')",track,academy);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/tripulaciones").with(admin).with(csrf()).param("semillero",track.toString()).param("nombre","Nova"))
                .andExpect(status().is3xxRedirection());
        UUID crew=jdbc.queryForObject("SELECT id FROM tripulacion WHERE nombre='Nova'",UUID.class);
        mvc.perform(post("/administracion/tripulaciones/miembros").with(admin).with(csrf()).param("tripulacion",crew.toString()).param("usuario",user.toString()).param("funcion","PARTICIPANTE"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("debe pertenecer al semillero")));
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",user.toString()).param("semillero",track.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/administracion/tripulaciones/miembros").with(admin).with(csrf()).param("tripulacion",crew.toString()).param("usuario",user.toString()).param("funcion","RESPONSABLE")).andExpect(status().is3xxRedirection());
        UUID assignment=jdbc.queryForObject("SELECT id FROM asignacion_semillero WHERE usuario_id=? AND semillero_id=?",UUID.class,user,track);
        UUID membership=jdbc.queryForObject("SELECT id FROM membresia_tripulacion WHERE usuario_id=? AND tripulacion_id=?",UUID.class,user,crew);
        mvc.perform(post("/administracion/asignaciones/cerrar").with(admin).with(csrf()).param("asignacion",assignment.toString())).andExpect(status().isOk()).andExpect(content().string(containsString("Primero cierra")));
        mvc.perform(post("/administracion/tripulaciones/miembros/cerrar").with(admin).with(csrf()).param("membresia",membership.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/administracion/asignaciones/cerrar").with(admin).with(csrf()).param("asignacion",assignment.toString())).andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM periodo_membresia WHERE membresia_id=? AND fin IS NOT NULL",Integer.class,membership));
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion='CERRAR_MEMBRESIA' AND recurso_id=?",Integer.class,membership));
    }

    @Test void teacherCanCreateAndAssignMissionOnlyInTheirTrack() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='PROFESOR'",UUID.randomUUID(),user);
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user), track=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ROB','Robótica','ACTIVO')",track,academy);
        var teacher=user("prueba@example.test").roles("PROFESOR");
        var create=post("/academia/misiones").with(teacher).with(csrf()).param("semillero",track.toString()).param("titulo","Sensor").param("instrucciones","Construye un sensor").param("criterio","Funcionamiento").param("modalidad","INDIVIDUAL").param("fecha",java.time.LocalDate.now().plusDays(10).toString());
        mvc.perform(create).andExpect(status().isForbidden());
        UUID assignment=UUID.randomUUID(),period=UUID.randomUUID();
        jdbc.update("INSERT INTO asignacion_semillero(id,academia_id,usuario_id,semillero_id) VALUES (?,?,?,?)",assignment,academy,user,track);
        jdbc.update("INSERT INTO periodo_asignacion(id,asignacion_id,inicio,registrado_por_id) VALUES (?,?,CURRENT_TIMESTAMP,?)",period,assignment,user);
        jdbc.update("INSERT INTO asignacion_activa(asignacion_id,periodo_id) VALUES (?,?)",assignment,period);
        mvc.perform(post("/academia/misiones").with(teacher).with(csrf()).param("semillero",track.toString()).param("titulo","Sensor").param("instrucciones","Construye un sensor").param("criterio","Funcionamiento").param("modalidad","INDIVIDUAL").param("fecha",java.time.LocalDate.now().plusDays(10).toString())).andExpect(status().is3xxRedirection());
        UUID mission=jdbc.queryForObject("SELECT id FROM mision WHERE titulo='Sensor'",UUID.class);
        mvc.perform(get("/academia/misiones").with(teacher)).andExpect(status().isOk()).andExpect(content().string(containsString("Sensor")));
        mvc.perform(post("/academia/misiones/asignar").with(teacher).with(csrf()).param("mision",mission.toString()).param("destinatario",user.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mis-misiones").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Construye un sensor")));
        mvc.perform(post("/academia/misiones/asignar").with(teacher).with(csrf()).param("mision",mission.toString()).param("destinatario",user.toString())).andExpect(status().isOk()).andExpect(content().string(containsString("ya está asignada")));
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM criterio_evaluacion WHERE mision_id=?",Integer.class,mission));
        UUID task=jdbc.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?",UUID.class,mission);
        String endpoint="/mis-misiones/"+task+"/entregas";
        mvc.perform(get(endpoint).with(user("ajeno@example.test").roles("ESTUDIANTE"))).andExpect(status().isForbidden());
        mvc.perform(post(endpoint).with(user("prueba@example.test").roles("ESTUDIANTE")).param("contenido","Sin protección")).andExpect(status().isForbidden());
        mvc.perform(post(endpoint).with(user("prueba@example.test").roles("ESTUDIANTE")).with(csrf()).param("contenido","Primera versión")).andExpect(status().is3xxRedirection());
        mvc.perform(post(endpoint).with(user("prueba@example.test").roles("ESTUDIANTE")).with(csrf()).param("contenido","Segunda versión")).andExpect(status().is3xxRedirection());
        mvc.perform(get(endpoint).with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Primera versión"))).andExpect(content().string(containsString("Segunda versión")));
        UUID delivery=jdbc.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?",UUID.class,task);
        mvc.perform(post("/academia/evaluaciones").with(user("ajeno@example.test").roles("PROFESOR")).with(csrf()).param("entrega",delivery.toString()).param("nota","4.0")).andExpect(status().isForbidden());
        mvc.perform(get("/academia/evaluaciones").with(teacher)).andExpect(status().isOk()).andExpect(content().string(containsString("Segunda versión")));
        mvc.perform(post("/academia/evaluaciones").with(teacher).with(csrf()).param("entrega",delivery.toString()).param("nota","4.0").param("observacion","Bien" )).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/evaluaciones").with(teacher).with(csrf()).param("entrega",delivery.toString()).param("nota","4.5").param("observacion","Corregida" )).andExpect(status().is3xxRedirection());
        mvc.perform(get(endpoint).with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Corregida")));
        org.junit.jupiter.api.Assertions.assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM version_evaluacion WHERE entrega_etapa_id=?",Integer.class,delivery));
        jdbc.update("UPDATE etapa_mision SET vencimiento=? WHERE mision_id=?",java.time.OffsetDateTime.now().minusDays(1),mission);
        mvc.perform(post(endpoint).with(user("prueba@example.test").roles("ESTUDIANTE")).with(csrf()).param("contenido","Tardía")).andExpect(status().isOk()).andExpect(content().string(containsString("fecha límite ha vencido")));
        org.junit.jupiter.api.Assertions.assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM version_entrega WHERE entrega_etapa_id=(SELECT id FROM entrega_etapa WHERE asignacion_id=?)",Integer.class,task));
        mvc.perform(post("/academia/autorizaciones").with(user("ajeno@example.test").roles("PROFESOR")).with(csrf()).param("asignacion",task.toString()).param("fecha",java.time.LocalDate.now().plusDays(1).toString()).param("motivo","Revisión")).andExpect(status().isForbidden());
        mvc.perform(get("/academia/autorizaciones").with(teacher)).andExpect(status().isOk()).andExpect(content().string(containsString("Sensor")));
        mvc.perform(post("/academia/autorizaciones").with(teacher).with(csrf()).param("asignacion",task.toString()).param("fecha",java.time.LocalDate.now().plusDays(1).toString()).param("motivo","Revisión")).andExpect(status().is3xxRedirection());
        mvc.perform(post(endpoint).with(user("prueba@example.test").roles("ESTUDIANTE")).with(csrf()).param("contenido","Autorizada")).andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM version_entrega WHERE entrega_etapa_id=? AND autorizacion_id IS NOT NULL",Integer.class,delivery));
    }

    @Test void stagesHaveIndependentDeliveriesAndCannotChangeAfterAssignment() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user), track=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'STAGE','Etapas','ACTIVO')",track,academy);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",user.toString()).param("semillero",track.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones").with(admin).with(csrf()).param("semillero",track.toString()).param("titulo","Prueba etapas").param("instrucciones","Desarrollar").param("criterio","Claridad").param("modalidad","INDIVIDUAL").param("fecha",java.time.LocalDate.now().plusDays(10).toString())).andExpect(status().is3xxRedirection());
        UUID mission=jdbc.queryForObject("SELECT id FROM mision WHERE titulo='Prueba etapas'",UUID.class);
        mvc.perform(post("/academia/misiones/etapas").with(admin).with(csrf()).param("mision",mission.toString()).param("titulo","Avance").param("criterio","Describe el diseño").param("fecha",java.time.LocalDate.now().plusDays(5).toString()).param("obligatoria","true")).andExpect(status().is3xxRedirection());
        UUID stage=jdbc.queryForObject("SELECT id FROM etapa_mision WHERE mision_id=? AND titulo='Avance'",UUID.class,mission);
        mvc.perform(post("/academia/misiones/asignar").with(admin).with(csrf()).param("mision",mission.toString()).param("destinatario",user.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones/etapas").with(admin).with(csrf()).param("mision",mission.toString()).param("titulo","Otra").param("criterio","Claridad").param("fecha",java.time.LocalDate.now().plusDays(5).toString())).andExpect(status().isOk()).andExpect(content().string(containsString("antes de asignar")));
        UUID task=jdbc.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?",UUID.class,mission);
        String endpoint="/mis-misiones/"+task+"/entregas";
        var student=user("prueba@example.test").roles("ESTUDIANTE");
        mvc.perform(get("/notificaciones").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("nueva Misión asignada")));
        mvc.perform(get("/").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("Ana"))).andExpect(content().string(org.hamcrest.Matchers.not(containsString("Prototipo: sensor de proximidad"))));
        UUID notice=jdbc.queryForObject("SELECT id FROM notificacion WHERE destinatario_id=? AND tipo='MISION_ASIGNADA'",UUID.class,user);
        mvc.perform(post("/notificaciones/leida").with(user("ajeno@example.test").roles("ESTUDIANTE")).with(csrf()).param("id",notice.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/notificaciones/leida").with(student).with(csrf()).param("id",notice.toString())).andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertNotNull(jdbc.queryForObject("SELECT leida_en FROM notificacion WHERE id=?",java.time.OffsetDateTime.class,notice));
        mvc.perform(post(endpoint).with(student).with(csrf()).param("contenido","Intento final anticipado")).andExpect(status().isOk()).andExpect(content().string(containsString("todas las etapas obligatorias")));
        org.junit.jupiter.api.Assertions.assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM version_entrega v JOIN entrega_etapa d ON d.id=v.entrega_etapa_id WHERE d.asignacion_id=?",Integer.class,task));
        mvc.perform(post(endpoint).with(student).with(csrf()).param("etapa",stage.toString()).param("contenido","Avance propio")).andExpect(status().is3xxRedirection());
        mvc.perform(post(endpoint).with(student).with(csrf()).param("contenido","Final propio")).andExpect(status().is3xxRedirection());
        UUID stageDelivery=jdbc.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=? AND etapa_id=?",UUID.class,task,stage);
        UUID finalDelivery=jdbc.queryForObject("SELECT d.id FROM entrega_etapa d JOIN etapa_mision e ON e.id=d.etapa_id WHERE d.asignacion_id=? AND e.es_final=TRUE",UUID.class,task);
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",stageDelivery.toString()).param("nota","6")).andExpect(status().isOk()).andExpect(content().string(containsString("entre 0 y 5")));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",stageDelivery.toString()).param("nota","2")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mis-misiones").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("Rendimiento insuficiente")));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",stageDelivery.toString()).param("nota","3")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",finalDelivery.toString()).param("nota","5")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mis-misiones").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("4,00")));
        mvc.perform(get("/notificaciones").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("evaluación o corrección")));
        mvc.perform(get("/clasificacion").with(student).param("semillero",track.toString())).andExpect(status().isOk()).andExpect(content().string(containsString(">80</td>")));
        mvc.perform(get("/administracion/clasificacion").with(student)).andExpect(status().isForbidden());
        mvc.perform(get("/administracion/clasificacion").with(admin)).andExpect(status().isOk()).andExpect(content().string(containsString(">80</td>")));
        mvc.perform(get("/").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("<span>80</span> XP"))).andExpect(content().string(containsString("Explorador")));
        mvc.perform(get("/clasificacion").with(student).param("semillero",UUID.randomUUID().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",stageDelivery.toString()).param("nota","4")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/clasificacion").with(student).param("semillero",track.toString())).andExpect(status().isOk()).andExpect(content().string(containsString(">90</td>"))).andExpect(content().string(org.hamcrest.Matchers.not(containsString(">170</td>"))));
        mvc.perform(get(endpoint).with(student).param("etapa",stage.toString())).andExpect(status().isOk()).andExpect(content().string(containsString("Describe el diseño"))).andExpect(content().string(containsString("Avance propio"))).andExpect(content().string(org.hamcrest.Matchers.not(containsString("Final propio"))));
        mvc.perform(get(endpoint).with(student).param("etapa",UUID.randomUUID().toString())).andExpect(status().isForbidden());
        mvc.perform(get("/academia/autorizaciones").with(admin)).andExpect(status().isOk()).andExpect(content().string(containsString("Avance")));
        jdbc.update("UPDATE etapa_mision SET vencimiento=? WHERE id=?",java.time.OffsetDateTime.now().minusDays(1),stage);
        mvc.perform(post(endpoint).with(student).with(csrf()).param("etapa",stage.toString()).param("contenido","Fuera de plazo")).andExpect(status().isOk()).andExpect(content().string(containsString("fecha límite ha vencido")));
        mvc.perform(post("/academia/autorizaciones").with(admin).with(csrf()).param("asignacion",task+":"+stage).param("fecha",java.time.LocalDate.now().plusDays(1).toString()).param("motivo","Revisión")).andExpect(status().is3xxRedirection());
        mvc.perform(post(endpoint).with(student).with(csrf()).param("etapa",stage.toString()).param("contenido","Avance autorizado")).andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM version_entrega v JOIN entrega_etapa e ON e.id=v.entrega_etapa_id WHERE e.etapa_id=? AND v.autorizacion_id IS NOT NULL",Integer.class,stage));
        UUID permission=jdbc.queryForObject("SELECT at.id FROM autorizacion_tardia at JOIN entrega_etapa d ON d.id=at.entrega_etapa_id WHERE d.etapa_id=?",UUID.class,stage);
        mvc.perform(post("/academia/autorizaciones/revocar").with(user("ajeno@example.test").roles("PROFESOR")).with(csrf()).param("autorizacion",permission.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/academia/autorizaciones/revocar").with(admin).with(csrf()).param("autorizacion",permission.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/notificaciones").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("Se revocó una autorización")));
        mvc.perform(post(endpoint).with(student).with(csrf()).param("etapa",stage.toString()).param("contenido","Después de revocar")).andExpect(status().isOk()).andExpect(content().string(containsString("fecha límite ha vencido")));
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM version_entrega WHERE autorizacion_id=?",Integer.class,permission));
        mvc.perform(get("/administracion/auditoria").with(student)).andExpect(status().isForbidden());
        mvc.perform(get("/administracion/auditoria").with(admin)).andExpect(status().isOk()).andExpect(content().string(containsString("REVOCAR_AUTORIZACION"))).andExpect(content().string(containsString("REGISTRAR_EVALUACION")));

    }

    @Test void competencyUsesLatestCriterionScoreAndRequiresAuthorizedTrack() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user), track=UUID.randomUUID(),other=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'COMP','Competencias','ACTIVO')",track,academy);
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'OTR','Otro','ACTIVO')",other,academy);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR");
        var student=user("prueba@example.test").roles("ESTUDIANTE");
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",user.toString()).param("semillero",track.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones").with(admin).with(csrf()).param("semillero",track.toString()).param("titulo","Misión competencia").param("instrucciones","Ejercicio").param("criterio","Diseño").param("modalidad","INDIVIDUAL").param("fecha",java.time.LocalDate.now().plusDays(10).toString())).andExpect(status().is3xxRedirection());
        UUID mission=jdbc.queryForObject("SELECT id FROM mision WHERE titulo='Misión competencia'",UUID.class);
        UUID criterion=jdbc.queryForObject("SELECT id FROM criterio_evaluacion WHERE mision_id=?",UUID.class,mission);
        mvc.perform(post("/academia/competencias").with(admin).with(csrf()).param("semillero",track.toString()).param("codigo","DISENO").param("nombre","Diseño técnico")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/competencias").with(admin).with(csrf()).param("semillero",other.toString()).param("codigo","AJENA").param("nombre","Otra")).andExpect(status().is3xxRedirection());
        UUID competence=jdbc.queryForObject("SELECT id FROM competencia WHERE codigo='DISENO'",UUID.class), foreign=jdbc.queryForObject("SELECT id FROM competencia WHERE codigo='AJENA'",UUID.class);
        mvc.perform(post("/academia/competencias/vincular").with(admin).with(csrf()).param("criterio",criterion.toString()).param("competencia",foreign.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/academia/competencias/vincular").with(admin).with(csrf()).param("criterio",criterion.toString()).param("competencia",competence.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones/asignar").with(admin).with(csrf()).param("mision",mission.toString()).param("destinatario",user.toString())).andExpect(status().is3xxRedirection());
        UUID task=jdbc.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?",UUID.class,mission);
        mvc.perform(post("/mis-misiones/"+task+"/entregas").with(student).with(csrf()).param("contenido","Entrega")).andExpect(status().is3xxRedirection());
        UUID delivery=jdbc.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?",UUID.class,task);
        mvc.perform(get("/academia/evaluaciones").with(admin)).andExpect(status().isOk()).andExpect(content().string(containsString("Diseño")));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4")).andExpect(status().isOk()).andExpect(content().string(containsString("Califica cada criterio")));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4").param("criterio_"+criterion,"2")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mi-progreso").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("Diseño técnico"))).andExpect(content().string(containsString("2,00"))).andExpect(content().string(containsString("por fortalecer")));
        var report=mvc.perform(get("/mi-boletin.pdf").with(student)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn().getResponse().getContentAsByteArray();
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(report)) {
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("Misión competencia"));
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("Diseño técnico"));
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("80 XP"));
        }
        mvc.perform(get("/mi-boletin.pdf")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/laboratorio/circuitos").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("Esquema de batería, interruptor, resistencia y LED conectados en serie")));
        mvc.perform(get("/laboratorio/circuitos")).andExpect(status().is3xxRedirection());

        mvc.perform(get("/academia/progreso").with(student)).andExpect(status().isForbidden());
        mvc.perform(get("/academia/progreso").with(admin)).andExpect(status().isOk()).andExpect(content().string(containsString("Diseño técnico"))).andExpect(content().string(containsString("2,00")));
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM alerta_rendimiento WHERE usuario_id=? AND estado='ABIERTA'",Integer.class,user));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4").param("criterio_"+criterion,"4.5")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mi-progreso").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("4,50"))).andExpect(content().string(not(containsString("por fortalecer"))));
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM alerta_rendimiento WHERE usuario_id=? AND estado='RESUELTA'",Integer.class,user));
        mvc.perform(get("/notificaciones").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("Una alerta de competencia fue resuelta")));
        org.junit.jupiter.api.Assertions.assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM puntaje_criterio WHERE criterio_id=?",Integer.class,criterion));
        mvc.perform(get("/mi-progreso").with(user("ajeno@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(not(containsString("Diseño técnico"))));
    }

    @Test void attachmentsAreValidatedAndRestrictedToParticipantsAndAuthorizedTeachers() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user),track=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'FILE','Adjuntos','ACTIVO')",track,academy);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR"); var student=user("prueba@example.test").roles("ESTUDIANTE");
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",user.toString()).param("semillero",track.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones").with(admin).with(csrf()).param("semillero",track.toString()).param("titulo","Misión adjunta").param("instrucciones","Ejercicio").param("criterio","Claridad").param("modalidad","INDIVIDUAL").param("fecha",java.time.LocalDate.now().plusDays(10).toString())).andExpect(status().is3xxRedirection());
        UUID mission=jdbc.queryForObject("SELECT id FROM mision WHERE titulo='Misión adjunta'",UUID.class);
        mvc.perform(post("/academia/misiones/asignar").with(admin).with(csrf()).param("mision",mission.toString()).param("destinatario",user.toString())).andExpect(status().is3xxRedirection());
        UUID task=jdbc.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?",UUID.class,mission);
        String endpoint="/mis-misiones/"+task+"/entregas";
        var bad=new org.springframework.mock.web.MockMultipartFile("adjunto","falso.pdf","application/pdf","no es pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(endpoint).file(bad).with(student).with(csrf()).param("contenido","Prueba")).andExpect(status().isOk()).andExpect(content().string(containsString("PDF, una imagen")));
        org.junit.jupiter.api.Assertions.assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM version_entrega v JOIN entrega_etapa d ON d.id=v.entrega_etapa_id WHERE d.asignacion_id=?",Integer.class,task));
        var oversized=new org.springframework.mock.web.MockMultipartFile("adjunto","grande.pdf","application/pdf",new byte[10*1024*1024+1]);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(endpoint).file(oversized).with(student).with(csrf()).param("contenido","Grande")).andExpect(status().isOk()).andExpect(content().string(containsString("supera el límite de 10 MB")));
        byte[] pdf="%PDF-1.4 contenido de prueba".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file=new org.springframework.mock.web.MockMultipartFile("adjunto","informe.pdf","application/pdf",pdf);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(endpoint).file(file).with(student).with(csrf()).param("contenido","Informe")).andExpect(status().is3xxRedirection());
        UUID attachment=jdbc.queryForObject("SELECT a.id FROM adjunto_entrega a JOIN version_entrega v ON v.id=a.version_id JOIN entrega_etapa d ON d.id=v.entrega_etapa_id WHERE d.asignacion_id=?",UUID.class,task);
        mvc.perform(get("/adjuntos/"+attachment).with(student)).andExpect(status().isOk()).andExpect(content().bytes(pdf));
        mvc.perform(get("/adjuntos/"+attachment).with(user("ajeno@example.test").roles("ESTUDIANTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/academia/evaluaciones").with(admin)).andExpect(status().isOk()).andExpect(content().string(containsString("informe.pdf")));
        mvc.perform(get("/adjuntos/"+attachment).with(admin)).andExpect(status().isOk()).andExpect(content().bytes(pdf));
    }

    @Test void interdisciplinaryAssignmentsAreScopedToTheirDestinationTrack() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user),a=UUID.randomUUID(),b=UUID.randomUUID(),teacherA=UUID.randomUUID(),teacherB=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'INTERA','Robótica','ACTIVO')",a,academy);
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'INTERB','Astronomía','ACTIVO')",b,academy);
        for(UUID teacher:new UUID[]{teacherA,teacherB}) {
            jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Docente','Prueba',?,'ACTIVO')",teacher,academy,teacher.equals(teacherA) ? "inter-a@example.test" : "inter-b@example.test",encoder.encode("clave-de-prueba"));
            jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='PROFESOR'",UUID.randomUUID(),teacher);
        }
        var admin=user("prueba@example.test").roles("ADMINISTRADOR"); var student=user("prueba@example.test").roles("ESTUDIANTE");
        var professorA=user("inter-a@example.test").roles("PROFESOR"); var professorB=user("inter-b@example.test").roles("PROFESOR");
        for(var pair:java.util.Map.of(user,b,teacherA,a,teacherB,b).entrySet()) mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",pair.getKey().toString()).param("semillero",pair.getValue().toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones").with(admin).with(csrf()).param("semillero",a.toString()).param("adicionales",b.toString()).param("titulo","Misión compartida").param("instrucciones","Investigar").param("criterio","Explicación").param("modalidad","INTERDISCIPLINARIA").param("fecha",java.time.LocalDate.now().plusDays(10).toString())).andExpect(status().is3xxRedirection());
        UUID mission=jdbc.queryForObject("SELECT id FROM mision WHERE titulo='Misión compartida'",UUID.class);
        org.junit.jupiter.api.Assertions.assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM mision_semillero WHERE mision_id=?",Integer.class,mission));
        mvc.perform(post("/academia/misiones/asignar").with(professorA).with(csrf()).param("mision",mission+":"+b).param("tipo","INDIVIDUAL").param("destinatario",user.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/academia/misiones/asignar").with(admin).with(csrf()).param("mision",mission+":"+b).param("tipo","INDIVIDUAL").param("destinatario",user.toString())).andExpect(status().is3xxRedirection());
        UUID task=jdbc.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?",UUID.class,mission);
        var pdf=new org.springframework.mock.web.MockMultipartFile("adjunto","inter.pdf","application/pdf","%PDF-1.4 test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/mis-misiones/"+task+"/entregas").file(pdf).with(student).with(csrf()).param("contenido","Entrega privada de B")).andExpect(status().is3xxRedirection());
        UUID delivery=jdbc.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?",UUID.class,task),attachment=jdbc.queryForObject("SELECT a.id FROM adjunto_entrega a JOIN version_entrega v ON v.id=a.version_id WHERE v.entrega_etapa_id=?",UUID.class,delivery);
        mvc.perform(get("/academia/evaluaciones").with(professorA)).andExpect(status().isOk()).andExpect(content().string(not(containsString("Entrega privada de B"))));
        mvc.perform(post("/academia/evaluaciones").with(professorA).with(csrf()).param("entrega",delivery.toString()).param("nota","4")).andExpect(status().isForbidden());
        mvc.perform(get("/adjuntos/"+attachment).with(professorA)).andExpect(status().isForbidden());
        mvc.perform(post("/academia/evaluaciones").with(professorB).with(csrf()).param("entrega",delivery.toString()).param("nota","4")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/academia/boletines/"+user+".pdf").with(professorA)).andExpect(status().isForbidden());
        mvc.perform(get("/academia/boletines/"+user+".pdf").with(professorB)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/academia/boletines/"+user+".pdf").with(admin)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));

        mvc.perform(post("/mis-misiones/"+task+"/entregas").with(student).with(csrf()).param("contenido","Versión final revisada")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/cierres").with(professorA).with(csrf()).param("asignacion",task.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/academia/cierres").with(professorB).with(csrf()).param("asignacion",task.toString())).andExpect(status().isOk()).andExpect(content().string(containsString("versión final más reciente")));
        mvc.perform(post("/academia/evaluaciones").with(professorB).with(csrf()).param("entrega",delivery.toString()).param("nota","4.5")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/cierres").with(professorB).with(csrf()).param("asignacion",task.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mis-misiones").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("CERRADA")));
        mvc.perform(get("/mis-misiones/"+task+"/entregas").with(student)).andExpect(status().isOk()).andExpect(content().string(containsString("Versión final revisada")));
        mvc.perform(post("/mis-misiones/"+task+"/entregas").with(student).with(csrf()).param("contenido","Ya cerrada")).andExpect(status().isOk()).andExpect(content().string(containsString("no admite nuevas entregas")));

        mvc.perform(post("/administracion/tripulaciones").with(admin).with(csrf()).param("semillero",b.toString()).param("nombre","Tripulación B")).andExpect(status().is3xxRedirection());
        UUID crew=jdbc.queryForObject("SELECT id FROM tripulacion WHERE nombre='Tripulación B'",UUID.class);
        mvc.perform(post("/administracion/tripulaciones/miembros").with(admin).with(csrf()).param("tripulacion",crew.toString()).param("usuario",user.toString()).param("funcion","PARTICIPANTE")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones/asignar").with(professorB).with(csrf()).param("mision",mission+":"+b).param("tipo","GRUPAL").param("destinatario",crew.toString())).andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM asignacion_mision WHERE mision_id=? AND semillero_destino_id=?",Integer.class,mission,b));
    }

    @Test void personalReportIncludesMoreThanOneHundredMissionsAcrossPages() throws Exception {
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user),track=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'PDFALL','Informes','ACTIVO')",track,academy);
        for(int n=0;n<101;n++) {
            UUID mission=UUID.randomUUID(),assignment=UUID.randomUUID();
            jdbc.update("INSERT INTO mision(id,academia_id,titulo,instrucciones,modalidad,estado,creada_por_id,politica_evaluacion) VALUES (?,?,?,'Ejercicio','INDIVIDUAL','PUBLICADA',?,'POR_ETAPA')",mission,academy,String.format("Informe %03d",n),user);
            jdbc.update("INSERT INTO mision_semillero(id,academia_id,mision_id,semillero_id) VALUES (?,?,?,?)",UUID.randomUUID(),academy,mission,track);
            jdbc.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,destinatario_usuario_id,semillero_destino_id,estado) VALUES (?,?,?,?,?,'ACTIVA')",assignment,academy,mission,user,track);
            jdbc.update("INSERT INTO participante_asignacion(id,asignacion_id,usuario_id,incorporado_en) VALUES (?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),assignment,user);
        }
        mvc.perform(get("/mis-misiones").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Siguientes"))).andExpect(content().string(not(containsString("Informe 100"))));
        mvc.perform(get("/mis-misiones").param("pagina","2").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Informe 100")));
        var bytes=mvc.perform(get("/mi-boletin.pdf").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)) {
            org.junit.jupiter.api.Assertions.assertTrue(pdf.getNumberOfPages()>1);
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("Informe 100"));
        }
    }

    @Test void groupMissionSnapshotsCrewParticipants() throws Exception {
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ADMINISTRADOR'",UUID.randomUUID(),user);
        UUID academy=jdbc.queryForObject("SELECT academia_id FROM usuario WHERE id=?",UUID.class,user), track=UUID.randomUUID();
        jdbc.update("INSERT INTO semillero(id,academia_id,codigo,nombre,estado) VALUES (?,?,'ESP','Espacio','ACTIVO')",track,academy);
        var admin=user("prueba@example.test").roles("ADMINISTRADOR");
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",user.toString()).param("semillero",track.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/administracion/tripulaciones").with(admin).with(csrf()).param("semillero",track.toString()).param("nombre","Equipo Nova")).andExpect(status().is3xxRedirection());
        UUID crew=jdbc.queryForObject("SELECT id FROM tripulacion WHERE nombre='Equipo Nova'",UUID.class);
        mvc.perform(post("/administracion/tripulaciones/miembros").with(admin).with(csrf()).param("tripulacion",crew.toString()).param("usuario",user.toString()).param("funcion","PARTICIPANTE")).andExpect(status().is3xxRedirection());
        UUID partner=UUID.randomUUID();
        jdbc.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,'compa@example.test','Compa','Prueba',?,'ACTIVO')",partner,academy,encoder.encode("clave-de-prueba"));
        jdbc.update("INSERT INTO usuario_rol(id,usuario_id,rol_id) SELECT ?,?,id FROM rol WHERE codigo='ESTUDIANTE'",UUID.randomUUID(),partner);
        mvc.perform(post("/administracion/asignaciones").with(admin).with(csrf()).param("usuario",partner.toString()).param("semillero",track.toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/administracion/tripulaciones/miembros").with(admin).with(csrf()).param("tripulacion",crew.toString()).param("usuario",partner.toString()).param("funcion","PARTICIPANTE")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/academia/misiones").with(admin).with(csrf()).param("semillero",track.toString()).param("titulo","Explorar").param("instrucciones","Colaborar").param("criterio","Claridad").param("modalidad","GRUPAL").param("fecha",java.time.LocalDate.now().plusDays(8).toString())).andExpect(status().is3xxRedirection());
        UUID mission=jdbc.queryForObject("SELECT id FROM mision WHERE titulo='Explorar'",UUID.class);
        mvc.perform(post("/academia/misiones/asignar").with(admin).with(csrf()).param("mision",mission.toString()).param("destinatario",crew.toString())).andExpect(status().is3xxRedirection());
        UUID task=jdbc.queryForObject("SELECT id FROM asignacion_mision WHERE mision_id=?",UUID.class,mission);
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM participante_asignacion WHERE asignacion_id=? AND usuario_id=?",Integer.class,task,user));
        mvc.perform(get("/mis-misiones").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Explorar")));
        mvc.perform(post("/mis-misiones/"+task+"/entregas").with(user("prueba@example.test").roles("ESTUDIANTE")).with(csrf()).param("contenido","Entrega conjunta")).andExpect(status().is3xxRedirection());
        UUID delivery=jdbc.queryForObject("SELECT id FROM entrega_etapa WHERE asignacion_id=?",UUID.class,task);
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4.5").param("ajuste_"+user,"1").param("justificacion_"+user,"Contribución")).andExpect(status().isOk()).andExpect(content().string(containsString("entre 0 y 5")));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4").param("ajuste_"+user,"0.5").param("justificacion_"+user,"Contribución destacada")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mis-misiones/"+task+"/entregas").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Contribución destacada")));
        mvc.perform(get("/mis-misiones").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("4,50")));
        mvc.perform(get("/mis-misiones/"+task+"/entregas").with(user("compa@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(not(containsString("Contribución destacada"))));
        mvc.perform(get("/mis-misiones").with(user("compa@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("4,00")));
        mvc.perform(get("/clasificacion").with(user("prueba@example.test").roles("ESTUDIANTE")).param("semillero",track.toString())).andExpect(status().isOk()).andExpect(content().string(containsString(">90</td>")));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4.5")).andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM ajuste_individual WHERE usuario_id=?",Integer.class,user));
        mvc.perform(get("/mis-misiones").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("5,00")));
        mvc.perform(get("/").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Primera Misión"))).andExpect(content().string(containsString("Nota perfecta")));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Primera Misión"))).andExpect(content().string(not(containsString("Nota perfecta"))));
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM evento_insignia ei JOIN insignia_usuario iu ON iu.id=ei.insignia_usuario_id JOIN insignia i ON i.id=iu.insignia_id WHERE iu.usuario_id=? AND i.codigo='NOTA_PERFECTA' AND ei.accion='REVOCADA'",Integer.class,user));
        mvc.perform(post("/academia/evaluaciones").with(admin).with(csrf()).param("entrega",delivery.toString()).param("nota","4.5")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/").with(user("prueba@example.test").roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("Nota perfecta")));
        org.junit.jupiter.api.Assertions.assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM evento_insignia ei JOIN insignia_usuario iu ON iu.id=ei.insignia_usuario_id JOIN insignia i ON i.id=iu.insignia_id WHERE iu.usuario_id=? AND i.codigo='NOTA_PERFECTA' AND ei.accion='REACTIVADA'",Integer.class,user));

        java.util.List<UUID> extras=new java.util.ArrayList<>();
        for(int index=0;index<4;index++) {
            UUID extraMission=UUID.randomUUID(),extraStage=UUID.randomUUID(),extraTask=UUID.randomUUID(),extraDelivery=UUID.randomUUID(),extraVersion=UUID.randomUUID(),extraEvaluation=UUID.randomUUID(); extras.add(extraMission);
            jdbc.update("INSERT INTO mision(id,academia_id,titulo,instrucciones,modalidad,estado,creada_por_id,politica_evaluacion) VALUES (?,?,?,'Ejercicio','INDIVIDUAL','PUBLICADA',?,'POR_ETAPA')",extraMission,academy,"Extra "+index,user);
            jdbc.update("INSERT INTO mision_semillero(id,academia_id,mision_id,semillero_id) VALUES (?,?,?,?)",UUID.randomUUID(),academy,extraMission,track);
            jdbc.update("INSERT INTO etapa_mision(id,mision_id,titulo,orden,obligatoria,es_final) VALUES (?,?,'Final',1,TRUE,TRUE)",extraStage,extraMission);
            jdbc.update("INSERT INTO asignacion_mision(id,academia_id,mision_id,destinatario_usuario_id,semillero_destino_id,estado) VALUES (?,?,?,?,?,'ACTIVA')",extraTask,academy,extraMission,user,track);
            jdbc.update("INSERT INTO participante_asignacion(id,asignacion_id,usuario_id,incorporado_en) VALUES (?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),extraTask,user);
            jdbc.update("INSERT INTO entrega_etapa(id,mision_id,asignacion_id,etapa_id) VALUES (?,?,?,?)",extraDelivery,extraMission,extraTask,extraStage);
            jdbc.update("INSERT INTO version_entrega(id,entrega_etapa_id,numero,autor_id,contenido,enviada_en) VALUES (?,?,1,?,'texto',CURRENT_TIMESTAMP)",extraVersion,extraDelivery,user);
            jdbc.update("INSERT INTO evaluacion(id,entrega_etapa_id) VALUES (?,?)",extraEvaluation,extraDelivery);
            jdbc.update("INSERT INTO version_evaluacion(id,evaluacion_id,entrega_etapa_id,numero,version_entrega_id,evaluador_id,calificacion_base,registrada_en) VALUES (?,?,?,1,?,?,4,CURRENT_TIMESTAMP)",UUID.randomUUID(),extraEvaluation,extraDelivery,extraVersion,user);
        }
        org.junit.jupiter.api.Assertions.assertTrue(badges.mine("prueba@example.test").stream().anyMatch(b->b.name().equals("Cinco Misiones")));
        for(UUID extra:extras) jdbc.update("UPDATE mision SET estado='CANCELADA' WHERE id=?",extra);
        org.junit.jupiter.api.Assertions.assertTrue(badges.mine("prueba@example.test").stream().anyMatch(b->b.name().equals("Cinco Misiones")));



    }

}
