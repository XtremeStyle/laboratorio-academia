package com.laboratorio.course;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CourseService {
    private final JdbcTemplate db;
    private final com.laboratorio.audit.AuditService audit;
    public CourseService(JdbcTemplate db, com.laboratorio.audit.AuditService audit) {
        this.db = db;
        this.audit = audit;
    }

    private Actor student(String email) {
        var rows = db.query("""
            SELECT u.id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO'
              AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id
                          WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE')
            """, (rs, i) -> new Actor(rs.getObject(1, UUID.class)), email);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return rows.getFirst();
    }

    /** Docente o administrador que consulta el catálogo dentro de su ámbito autorizado. */
    private record Viewer(UUID id, UUID academy, boolean admin) {}

    private Viewer viewer(String email) {
        var rows = db.query("""
            SELECT u.id,u.academia_id,
              EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id
                      WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE') AS estud,
              EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id
                      WHERE ur.usuario_id=u.id AND r.codigo='PROFESOR') AS prof,
              EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id
                      WHERE ur.usuario_id=u.id AND r.codigo='ADMINISTRADOR') AS adm
            FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO'
            """, (rs, i) -> new Object[] { rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                rs.getBoolean(3), rs.getBoolean(4), rs.getBoolean(5) }, email);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Object[] row = rows.getFirst();
        boolean profesor = (Boolean) row[3], admin = (Boolean) row[4];
        if (!(profesor || admin)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return new Viewer((UUID) row[0], (UUID) row[1], admin);
    }

    /** Catálogo de solo lectura para docentes y administradores: sin matrícula ni acciones de estudio. */
    @Transactional(readOnly = true)
    public List<CourseCard> staffCatalog(String email) {
        Viewer viewer = viewer(email);
        String scope = viewer.admin()
            ? " s.academia_id=?"
            : " EXISTS (SELECT 1 FROM asignacion_semillero a JOIN asignacion_activa aa ON aa.asignacion_id=a.id "
              + "              WHERE a.usuario_id=? AND a.semillero_id=s.id)";
        return db.query("""
            SELECT c.id,c.codigo,c.titulo,c.descripcion,c.nivel,c.duracion_minutos,
              s.id,s.nombre,CAST(NULL AS UUID),
              (SELECT COUNT(*) FROM leccion_curso l WHERE l.curso_id=c.id),0,CAST(NULL AS VARCHAR)
            FROM curso c JOIN curso_semillero cs ON cs.curso_id=c.id
            JOIN semillero s ON s.id=cs.semillero_id
            WHERE c.estado='PUBLICADO' AND s.estado='ACTIVO' AND """ + scope + "\n" + """
            ORDER BY c.titulo,s.nombre
            """, (rs, i) -> new CourseCard(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),
                rs.getString(4),rs.getString(5),rs.getInt(6),rs.getObject(7,UUID.class),rs.getString(8),null,
                rs.getInt(10),0,null), viewer.admin() ? viewer.academy() : viewer.id());
    }

    /** Vista del catálogo según el rol: estudiante con matrículas o docente/administrador de solo lectura. */
    @Transactional(readOnly = true)
    public CatalogView catalogFor(String email) {
        try {
            var personal = catalog(email);
            try { return new CatalogView(personal, true, staffCatalog(email)); }
            catch (ResponseStatusException noStaffRole) { return new CatalogView(personal, true, List.of()); }
        } catch (ResponseStatusException forbidden) {
            return new CatalogView(staffCatalog(email), false, List.of());
        }
    }

    public record CatalogView(List<CourseCard> courses, boolean student, List<CourseCard> staffCourses) {}

    @Transactional(readOnly = true)
    public StaffProgressPage staffProgress(String email, UUID courseId, UUID trackId, int page) {
        if(page<0 || page>100000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Viewer staff=viewer(email);
        Integer allowed=db.queryForObject("""
            SELECT COUNT(*) FROM curso_semillero cs JOIN semillero s ON s.id=cs.semillero_id
            WHERE cs.curso_id=? AND s.id=? AND s.academia_id=? AND s.estado='ACTIVO'
              AND (?=TRUE OR EXISTS (SELECT 1 FROM asignacion_semillero a
                JOIN asignacion_activa aa ON aa.asignacion_id=a.id WHERE a.usuario_id=? AND a.semillero_id=s.id))
            """,Integer.class,courseId,trackId,staff.academy(),staff.admin(),staff.id());
        if(allowed==null || allowed!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var rows=db.query("""
            SELECT u.nombre,u.apellido,mc.estado,l.numero,l.titulo,COALESCE(p.estado,'NO_INICIADA'),p.evidencia
            FROM matricula_curso mc JOIN usuario u ON u.id=mc.usuario_id
            JOIN leccion_curso l ON l.curso_id=mc.curso_id
            LEFT JOIN progreso_leccion p ON p.matricula_id=mc.id AND p.leccion_id=l.id
            WHERE mc.curso_id=? AND mc.semillero_id=? AND u.academia_id=?
            ORDER BY u.apellido,u.nombre,u.id,l.numero LIMIT 101 OFFSET ?
            """,(rs,i)->new StaffProgress(rs.getString(1),rs.getString(2),rs.getString(3),rs.getInt(4),rs.getString(5),rs.getString(6),rs.getString(7)),courseId,trackId,staff.academy(),page*100);
        return new StaffProgressPage(List.copyOf(rows.subList(0,Math.min(rows.size(),100))),page,rows.size()>100);
    }

    public record StaffProgress(String name,String surname,String enrollmentState,int lessonNumber,String lessonTitle,String lessonState,String evidence) {}
    public record StaffProgressPage(List<StaffProgress> rows,int page,boolean hasNext) {}

    @Transactional(readOnly = true)
    public List<CourseCard> catalog(String email) {
        Actor actor = student(email);
        return db.query("""
            SELECT c.id,c.codigo,c.titulo,c.descripcion,c.nivel,c.duracion_minutos,
              s.id,s.nombre,mc.id,
              (SELECT COUNT(*) FROM leccion_curso l WHERE l.curso_id=c.id),
              (SELECT COUNT(*) FROM progreso_leccion p WHERE p.matricula_id=mc.id AND p.estado='COMPLETADA'),
              mc.estado
            FROM curso c JOIN curso_semillero cs ON cs.curso_id=c.id
            JOIN semillero s ON s.id=cs.semillero_id
            JOIN asignacion_semillero a ON a.semillero_id=s.id AND a.usuario_id=?
            JOIN asignacion_activa aa ON aa.asignacion_id=a.id
            LEFT JOIN matricula_curso mc ON mc.curso_id=c.id AND mc.usuario_id=? AND mc.semillero_id=s.id
            WHERE c.estado='PUBLICADO' AND s.estado='ACTIVO'
            ORDER BY c.titulo,s.nombre
            """, (rs, i) -> new CourseCard(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),
                rs.getString(4),rs.getString(5),rs.getInt(6),rs.getObject(7,UUID.class),rs.getString(8),rs.getObject(9,UUID.class),
                rs.getInt(10),rs.getInt(11),rs.getString(12)), actor.id(), actor.id());
    }

    @Transactional
    public void enroll(String email, UUID courseId, UUID trackId) {
        Actor actor = student(email);
        Integer allowed = db.queryForObject("""
            SELECT COUNT(*) FROM curso c JOIN curso_semillero cs ON cs.curso_id=c.id
            JOIN semillero s ON s.id=cs.semillero_id
            JOIN asignacion_semillero a ON a.semillero_id=s.id AND a.usuario_id=?
            JOIN asignacion_activa aa ON aa.asignacion_id=a.id
            WHERE c.id=? AND s.id=? AND c.estado='PUBLICADO' AND s.estado='ACTIVO'
            """, Integer.class, actor.id(), courseId, trackId);
        if (allowed == null || allowed != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // Serializa matrículas del mismo estudiante para que dos solicitudes concurrentes
        // no compitan por la restricción UNIQUE (curso_id, usuario_id, semillero_id).
        db.query("SELECT id FROM usuario WHERE id=? FOR UPDATE",
            (rs, i) -> rs.getObject(1, UUID.class), actor.id());
        UUID enrollment = UUID.randomUUID();
        int created = db.update("""
            INSERT INTO matricula_curso(id,curso_id,usuario_id,semillero_id,estado)
            SELECT ?,?,?,?,'ACTIVA' WHERE NOT EXISTS
              (SELECT 1 FROM matricula_curso WHERE curso_id=? AND usuario_id=? AND semillero_id=?)
            """, enrollment, courseId, actor.id(), trackId, courseId, actor.id(), trackId);
        UUID actualEnrollment = db.queryForObject(
            "SELECT id FROM matricula_curso WHERE curso_id=? AND usuario_id=? AND semillero_id=?",
            UUID.class, courseId, actor.id(), trackId);
        List<UUID> lessonIds = db.query("SELECT id FROM leccion_curso WHERE curso_id=? ORDER BY numero",
            (rs, i) -> rs.getObject(1, UUID.class), courseId);
        for (UUID lessonId : lessonIds) {
            db.update("""
                INSERT INTO progreso_leccion(id,matricula_id,curso_id,leccion_id,estado)
                SELECT ?,?,?,?,'NO_INICIADA' WHERE NOT EXISTS
                  (SELECT 1 FROM progreso_leccion WHERE matricula_id=? AND leccion_id=?)
                """, UUID.randomUUID(), actualEnrollment, courseId, lessonId, actualEnrollment, lessonId);
        }
        if (created == 1) audit.record(actor.id(), "MATRICULAR_CURSO", "MATRICULA_CURSO", actualEnrollment);
    }

    @Transactional
    public void updateLesson(String email, UUID enrollmentId, UUID lessonId, String state, String evidence) {
        Actor actor = student(email);
        if (!List.of("EN_CURSO", "COMPLETADA").contains(state))
            throw new IllegalArgumentException("Estado de lección no válido.");
        String proof = evidence == null ? "" : evidence.strip();
        if (proof.length() > 5000)
            throw new IllegalArgumentException("La evidencia no puede superar los 5000 caracteres.");
        if (state.equals("COMPLETADA") && (proof.length() < 12 || proof.length() > 5000))
            throw new IllegalArgumentException("Para completar la lección, registra una evidencia de entre 12 y 5000 caracteres.");
        // El bloqueo de matrícula serializa actualizaciones de lecciones distintas y evita
        // que dos últimas lecciones simultáneas dejen el curso sin cerrar.
        var owned = db.query("SELECT id FROM matricula_curso WHERE id=? AND usuario_id=? AND estado='ACTIVA' FOR UPDATE",
            (rs, i) -> rs.getObject(1, UUID.class), enrollmentId, actor.id());
        if (owned.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        // Si el estudiante perdió su asignación activa al semillero, se revoca también la escritura
        // en las lecciones: ni el semillero puede estar inactivo ni la asignación puede estar cerrada.
        var active = db.query("""
            SELECT 1 FROM matricula_curso mc JOIN semillero s ON s.id=mc.semillero_id
            WHERE mc.id=? AND s.estado='ACTIVO'
              AND EXISTS (SELECT 1 FROM asignacion_semillero a JOIN asignacion_activa aa ON aa.asignacion_id=a.id
                          WHERE a.usuario_id=? AND a.semillero_id=mc.semillero_id)
            """, (rs, i) -> true, enrollmentId, actor.id());
        if (active.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        ensureLessonProgress(enrollmentId);
        var current = db.query("SELECT estado FROM progreso_leccion WHERE matricula_id=? AND leccion_id=?",
            (rs, i) -> rs.getString(1), enrollmentId, lessonId);
        if (current.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        // El progreso es monotónico: una lección completada no vuelve a un estado anterior.
        if (current.getFirst().equals("COMPLETADA") && state.equals("EN_CURSO")) return;
        int changed = db.update("""
            UPDATE progreso_leccion SET estado=?, evidencia=?,
              iniciado_en=COALESCE(iniciado_en,CURRENT_TIMESTAMP),
              completado_en=CASE WHEN ?='COMPLETADA' THEN COALESCE(completado_en,CURRENT_TIMESTAMP) ELSE NULL END,
              actualizado_en=CURRENT_TIMESTAMP
            WHERE matricula_id=? AND leccion_id=?
            """, state, proof.isEmpty() ? null : proof, state, enrollmentId, lessonId);
        if (changed != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Integer pending = db.queryForObject("""
            SELECT COUNT(*) FROM progreso_leccion p JOIN leccion_curso l ON l.id=p.leccion_id
            WHERE p.matricula_id=? AND l.obligatoria=TRUE AND p.estado<>'COMPLETADA'
            """, Integer.class, enrollmentId);
        if (pending != null && pending == 0) {
            db.update("UPDATE matricula_curso SET estado='COMPLETADA',actualizada_en=CURRENT_TIMESTAMP WHERE id=? AND estado='ACTIVA'", enrollmentId);
        }
        if (state.equals("COMPLETADA"))
            audit.record(actor.id(), "COMPLETAR_LECCION", "PROGRESO_LECCION", lessonId);
    }

    private void ensureLessonProgress(UUID enrollmentId) {
        var missing = db.query("""
            SELECT l.id,l.curso_id FROM matricula_curso mc JOIN leccion_curso l ON l.curso_id=mc.curso_id
            WHERE mc.id=? AND NOT EXISTS (SELECT 1 FROM progreso_leccion p WHERE p.matricula_id=mc.id AND p.leccion_id=l.id)
            """, (rs,i) -> new UUID[]{rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)}, enrollmentId);
        for (UUID[] lesson : missing) db.update("INSERT INTO progreso_leccion(id,matricula_id,curso_id,leccion_id,estado) VALUES (?,?,?,?,'NO_INICIADA')",UUID.randomUUID(),enrollmentId,lesson[1],lesson[0]);
    }

    @Transactional
    public CourseDetail detail(String email, UUID enrollmentId) {
        Actor actor = student(email);
        var courses = db.query("""
            SELECT c.id,c.titulo,c.descripcion,s.nombre,mc.id,mc.estado
            FROM matricula_curso mc JOIN curso c ON c.id=mc.curso_id
            JOIN semillero s ON s.id=mc.semillero_id
            WHERE mc.id=? AND mc.usuario_id=?
              -- El acceso de consulta también exige asignación activa y semillero activo.
              AND s.estado='ACTIVO'
              AND EXISTS (SELECT 1 FROM asignacion_semillero a JOIN asignacion_activa aa ON aa.asignacion_id=a.id
                          WHERE a.usuario_id=mc.usuario_id AND a.semillero_id=mc.semillero_id)
            """, (rs,i)->new CourseHead(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getObject(5,UUID.class),rs.getString(6)), enrollmentId, actor.id());
        if (courses.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        CourseHead head = courses.getFirst();
        // Lock before adding progress so concurrent requests cannot create duplicate rows.
        db.query("SELECT id FROM matricula_curso WHERE id=? FOR UPDATE", (rs,i) -> rs.getObject(1,UUID.class), enrollmentId);
        ensureLessonProgress(enrollmentId);
        var lessons = db.query("""
            SELECT l.id,l.numero,l.titulo,l.objetivo,l.contenido,l.actividad,l.evidencia,l.duracion_minutos,
              p.estado,p.evidencia
            FROM leccion_curso l JOIN progreso_leccion p ON p.leccion_id=l.id AND p.curso_id=l.curso_id
            WHERE p.matricula_id=? ORDER BY l.numero
            """,(rs,i)->new Lesson(rs.getObject(1,UUID.class),rs.getInt(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getInt(8),rs.getString(9),rs.getString(10)),enrollmentId);
        return new CourseDetail(head,List.copyOf(lessons));
    }

    private record Actor(UUID id) {}
    public record CourseHead(UUID id,String title,String description,String track,UUID enrollment,String state) {}
    public record CourseCard(UUID id,String code,String title,String description,String level,int minutes,UUID trackId,String track,UUID enrollment,int lessons,int completed,String state) {}
    public record Lesson(UUID id,int number,String title,String objective,String content,String activity,String evidence,int minutes,String state,String submittedEvidence) {}
    public record CourseDetail(CourseHead course,List<Lesson> lessons) {}
}
