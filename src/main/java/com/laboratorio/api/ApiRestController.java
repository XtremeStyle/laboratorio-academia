package com.laboratorio.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST del backend: endpoints JSON estables que envuelven los servicios de la aplicación
 * (mismo ámbito y autorización que la web). Exclusivamente autenticados.
 */
@Tag(name = "CHANGE API", description = "Backend REST de CHANGE // SPACE-LAB OS")
@SecurityRequirement(name = "session")
@RestController
@RequestMapping("/api")
public class ApiRestController {
    private final JdbcTemplate db;
    private final com.laboratorio.course.CourseService courses;
    private final com.laboratorio.mission.MissionService missions;
    private final com.laboratorio.notification.NotificationService notifications;
    private final com.laboratorio.progress.ProgressService progress;
    private final com.laboratorio.gamification.RankingService ranking;
    private final com.laboratorio.gamification.XpMovementService xp;

    public ApiRestController(JdbcTemplate db,
                             com.laboratorio.course.CourseService courses,
                             com.laboratorio.mission.MissionService missions,
                             com.laboratorio.notification.NotificationService notifications,
                             com.laboratorio.progress.ProgressService progress,
                             com.laboratorio.gamification.RankingService ranking,
                             com.laboratorio.gamification.XpMovementService xp) {
        this.db = db;
        this.courses = courses;
        this.missions = missions;
        this.notifications = notifications;
        this.progress = progress;
        this.ranking = ranking;
        this.xp = xp;
    }

    @Operation(summary = "Perfil del solicitante", description = "Devuelve el correo, nombre completo y roles del usuario autenticado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil obtenido",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = java.util.Map.class))),
        @ApiResponse(responseCode = "302", description = "Redirigido a /login si no hay sesión")
    })
    @GetMapping("/perfil")
    public Map<String, Object> perfil(Principal principal) {
        var roles = roles(principal.getName());
        Map<String, Object> out = new HashMap<>();
        out.put("correo", principal.getName());
        out.put("nombre", nombre(principal.getName()));
        out.put("roles", roles);
        return out;
    }

    @Operation(summary = "Catálogo de Cursos visible", description = "Devuelve el catálogo para estudiantes (inscripción activa) y los cursos de consulta académica (docentes/admins).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Catálogo devuelto correctamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = java.util.Map.class))),
        @ApiResponse(responseCode = "302", description = "Redirigido a /login si no hay sesión")
    })
    @GetMapping("/cursos")
    public Map<String, Object> cursos(Principal principal) {
        var vista = courses.catalogFor(principal.getName());
        return Map.of(
                "estudiante", vista.student(),
                "cursos", vista.courses(),
                "consultaAcademica", vista.staffCourses());
    }

    @Operation(summary = "Detalle de una matrícula", description = "Devuelve la matrícula y el estado de sus lecciones si el solicitante es estudiante en ella.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalle devuelto"),
        @ApiResponse(responseCode = "403", description = "La matrícula no pertenece al solicitante o el rol no está autorizado"),
        @ApiResponse(responseCode = "404", description = "Matrícula inexistente"),
        @ApiResponse(responseCode = "302", description = "Redirigido a /login si no hay sesión")
    })
    @GetMapping("/cursos/{enrollmentId}")
    public Map<String, Object> detalle(@PathVariable UUID enrollmentId, Authentication auth) {
        boolean visible = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE"));
        if (!visible) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var detalle = courses.detail(auth.getName(), enrollmentId);
        if (detalle == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return Map.of("course", detalle.course(), "lessons", detalle.lessons());
    }

    @Operation(summary = "Misiones asignadas", description = "Lista las Misiones del solicitante con promedio vigente y estado actual.")
    @ApiResponse(responseCode = "200", description = "Lista de Misiones",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = java.util.List.class)))
    @GetMapping("/misiones")
    public List<Map<String, Object>> misiones(Principal principal) {
        return missions.mineForReport(principal.getName()).stream()
                .map(m -> Map.<String, Object>of(
                        "titulo", m.title(),
                        "modalidad", m.mode(),
                        "fechaLimite", m.deadline().toString(),
                        "promedio", m.average(),
                        "estado", m.state(),
                        "notasFueraDeRango", m.invalidGrades()))
                .toList();
    }

    @Operation(summary = "Notificaciones", description = "Devuelve las últimas 50 notificaciones del solicitante, con su estado de lectura.")
    @ApiResponse(responseCode = "200", description = "Notificaciones devueltas",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = java.util.List.class)))
    @GetMapping("/notificaciones")
    public List<Map<String, Object>> notificaciones(Principal principal) {
        return notifications.list(principal.getName()).stream()
                .limit(50)
                .map(n -> Map.<String, Object>of(
                        "id", n.id(),
                        "contenido", n.content(),
                        "creada", n.created().toString(),
                        "leida", n.read() != null))
                .toList();
    }

    @Operation(summary = "Progreso por competencias", description = "Promedio de las notas de los criterios vinculados a competencias por semillero.")
    @ApiResponse(responseCode = "200", description = "Progreso devuelto",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = java.util.List.class)))
    @GetMapping("/progreso")
    public List<Map<String, Object>> progreso(Principal principal) {
        return progress.mine(principal.getName()).stream()
                .map(c -> Map.<String, Object>of(
                        "competencia", c.name(),
                        "semillero", c.track(),
                        "promedio", c.average(),
                        "criterios", c.count()))
                .toList();
    }

    @Operation(summary = "Experiencia y rango", description = "Devuelve XP total, rango, número de Misiones y los últimos 10 movimientos del libro de XP.")
    @ApiResponse(responseCode = "200", description = "XP devuelto",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = java.util.Map.class)))
    @GetMapping("/xp")
    public Map<String, Object> xpPersonal(Principal principal) {
        var personal = ranking.personal(principal.getName());
        return Map.of(
                "xp", personal.xp(),
                "rango", personal.rank(),
                "misiones", personal.missions(),
                "movimientos", xp.movementsFor(principal.getName(), 10));
    }

    private List<String> roles(String email) {
        return db.query("SELECT r.codigo FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id JOIN usuario u ON u.id=ur.usuario_id WHERE u.correo=?",
                (rs, i) -> rs.getString(1), email);
    }

    private String nombre(String email) {
        var rows = db.query("SELECT nombre,apellido FROM usuario WHERE correo=?",
                (rs, i) -> rs.getString(1) + " " + rs.getString(2), email);
        return rows.isEmpty() ? "" : rows.getFirst();
    }
}
