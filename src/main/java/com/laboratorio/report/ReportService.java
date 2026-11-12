package com.laboratorio.report;

import com.laboratorio.gamification.BadgeService;
import com.laboratorio.gamification.RankingService;
import com.laboratorio.mission.MissionService;
import com.laboratorio.progress.ProgressService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {
    private static final java.awt.Color NAVY = new java.awt.Color(11, 16, 32);
    private static final java.awt.Color NAVY_2 = new java.awt.Color(22, 31, 53);
    private static final java.awt.Color CYAN = new java.awt.Color(0, 229, 255);
    private static final java.awt.Color ORANGE = new java.awt.Color(255, 158, 44);
    private static final java.awt.Color TEXT = new java.awt.Color(24, 32, 48);
    private static final java.awt.Color MUTED = new java.awt.Color(105, 120, 142);
    private static final java.awt.Color FAINT = new java.awt.Color(241, 246, 252);
    private static final java.awt.Color LINE = new java.awt.Color(222, 230, 240);

    private final JdbcTemplate db;
    private final MissionService missions;
    private final ProgressService progress;
    private final RankingService ranking;
    private final BadgeService badges;
    private final com.laboratorio.audit.AuditService audit;

    public ReportService(JdbcTemplate db, MissionService missions, ProgressService progress, RankingService ranking, BadgeService badges, com.laboratorio.audit.AuditService audit) {
        this.db = db;
        this.missions = missions;
        this.progress = progress;
        this.ranking = ranking;
        this.badges = badges;
        this.audit = audit;
    }

    @Transactional
    public byte[] mine(String email) throws IOException {
        var people = db.query("SELECT u.nombre,u.apellido,a.nombre FROM usuario u JOIN academia a ON a.id=u.academia_id WHERE u.correo=? AND u.estado='ACTIVO'", (rs, i) -> new Person(rs.getString(1) + " " + rs.getString(2), rs.getString(3)), email);
        if (people.size() != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var person = people.getFirst();
        var myMissions = missions.mineForReport(email);
        var competencies = progress.mine(email);
        var experience = ranking.personal(email);
        var awards = badges.mine(email);
        String fecha = java.time.ZonedDateTime.now(ZoneId.of("America/Bogota")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        try (PDDocument doc = new PDDocument(); var fontStream = ReportService.class.getResourceAsStream("/fonts/LiberationSans-Regular.ttf"); var bytes = new ByteArrayOutputStream()) {
            if (fontStream == null) throw new IOException("No se encontró la fuente del boletín.");
            PDType0Font font = PDType0Font.load(doc, fontStream);
            BulletinWriter writer = new BulletinWriter(doc, font);
            writer.begin();
            writer.identity(person.name(), person.academy(), fecha, experience.xp(), experience.rank());
            writer.section("Misiones y notas");
            if (myMissions.isEmpty()) writer.emptyNote("Aún no hay Misiones activas o completadas asignadas.");
            for (var m : myMissions) writer.missionRow(m, format(m.average()));
            writer.section("Competencias");
            if (competencies.isEmpty()) writer.emptyNote("Sin criterios de competencias evaluados.");
            for (var c : competencies) writer.competencyRow(c, format(c.average()));
            writer.section("Insignias");
            if (awards.isEmpty()) writer.emptyNote("Sin insignias vigentes.");
            for (var badge : awards) writer.badgeChip(badge.name(), badge.description());
            writer.note("El boletín refleja las evaluaciones registradas en el momento de su generación.");
            writer.close();
            doc.save(bytes);
            return bytes.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public List<Student> students(String actorEmail) {
        var actors = db.query("SELECT u.id,u.academia_id FROM usuario u WHERE u.correo=? AND u.estado='ACTIVO' AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo IN ('ADMINISTRADOR','PROFESOR'))", (rs, i) -> new Actor(rs.getObject(1, java.util.UUID.class), rs.getObject(2, java.util.UUID.class)), actorEmail);
        if (actors.size() != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var a = actors.getFirst();
        return db.query("""
          SELECT u.id,u.nombre,u.apellido,u.correo FROM usuario u WHERE u.academia_id=? AND u.estado='ACTIVO'
          AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE')
          AND (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ADMINISTRADOR')
            OR EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id JOIN semillero s ON s.id=x.semillero_id AND s.estado='ACTIVO'
              JOIN asignacion_semillero y ON y.semillero_id=x.semillero_id JOIN asignacion_activa ya ON ya.asignacion_id=y.id
              WHERE x.usuario_id=? AND y.usuario_id=u.id))
          ORDER BY u.apellido,u.nombre,u.id LIMIT 500
          """, (rs, i) -> new Student(rs.getObject(1, java.util.UUID.class), rs.getString(2) + " " + rs.getString(3), rs.getString(4)), a.academy(), a.id(), a.id());
    }

    @Transactional
    public byte[] forStudent(String actorEmail, java.util.UUID student) throws IOException {
        var permitted = db.query("""
          SELECT target.correo,actor.id FROM usuario actor JOIN usuario target ON target.academia_id=actor.academia_id
          WHERE actor.correo=? AND actor.estado='ACTIVO' AND target.id=? AND target.estado='ACTIVO'
            AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=target.id AND r.codigo='ESTUDIANTE')
            AND (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=actor.id AND r.codigo='ADMINISTRADOR')
              OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=actor.id AND r.codigo='PROFESOR')
                AND EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id JOIN semillero s ON s.id=x.semillero_id AND s.estado='ACTIVO'
                   JOIN asignacion_semillero y ON y.semillero_id=x.semillero_id JOIN asignacion_activa ya ON ya.asignacion_id=y.id
                   WHERE x.usuario_id=actor.id AND y.usuario_id=target.id)))
          """, (rs, i) -> new Target(rs.getString(1), rs.getObject(2, java.util.UUID.class)), actorEmail, student);
        if (permitted.size() != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        byte[] pdf = mine(permitted.getFirst().email());
        audit.record(permitted.getFirst().actor(), "DESCARGAR_BOLETIN", "USUARIO", student);
        return pdf;
    }

    private record Actor(java.util.UUID id, java.util.UUID academy) { }
    private record Target(String email, java.util.UUID actor) { }
    public record Student(java.util.UUID id, String name, String email) { }
    private String format(BigDecimal value) { return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','); }
    private record Person(String name, String academy) { }

    private static final class BulletinWriter implements AutoCloseable {
        private final PDDocument doc;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private PDPage page;
        private float y;
        private int pageNumber = 0;
        private static final float W = PDRectangle.LETTER.getWidth();
        private static final float H = PDRectangle.LETTER.getHeight();
        private static final float LEFT = 42;
        private static final float CONTENT_W = W - 2 * LEFT;

        BulletinWriter(PDDocument doc, PDType0Font font) { this.doc = doc; this.font = font; }

        private void page() throws IOException {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            pageNumber++;
            drawChrome(page);
            y = H - 106;
        }

        void begin() throws IOException { page(); }

        private void drawChrome(PDPage p) throws IOException {
            stream.setNonStrokingColor(NAVY);
            stream.addRect(0, H - 74, W, 74);
            stream.fill();
            stream.setNonStrokingColor(CYAN);
            stream.addRect(0, H - 77, W, 3f);
            stream.fill();
            navyText("CHANGE", 22, LEFT, H - 30, NAVY == null ? TEXT : new java.awt.Color(255, 255, 255));
            navyText("// SPACE-LAB OS", 9, W - 42 - 78, H - 35, CYAN, true);
            footer(p);
        }

        private void footer(PDPage p) throws IOException {
            stream.setNonStrokingColor(LINE);
            stream.addRect(LEFT, 40, CONTENT_W, 1f);
            stream.fill();
            greyText("CHANGE // SPACE-LAB OS · Change the way you learn.", 7, LEFT, 27, MUTED);
            greyText("pág. " + pageNumber, 7, W - LEFT - 30, 27, MUTED);
        }

        private void navyText(String text, float size, float x, float baseline, java.awt.Color color) throws IOException { navyText(text, size, x, baseline, color, false); }

        private void navyText(String text, float size, float x, float baseline, java.awt.Color color, boolean right) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            float tx = right ? x : x;
            stream.newLineAtOffset(tx, baseline);
            stream.showText(text);
            stream.endText();
        }

        private void greyText(String text, float size, float x, float baseline, java.awt.Color color) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(text);
            stream.endText();
        }

        private float width(String text, float size) { try { return font.getStringWidth(text) * size / 1000; } catch (Exception e) { return text.length() * size * 0.6f; } }

        void identity(String name, String academy, String fecha, int xp, String rank) throws IOException {
            navyText("BOLETÍN PERSONAL", 12, LEFT, y - 4, MUTED);
            navyText(name, 23, LEFT, y - 30, NAVY);
            greyText(academy + "  ·  Generado: " + fecha + " (América/Bogotá)", 9, LEFT, y - 51, MUTED);
            // Bloque XP
            stream.setNonStrokingColor(FAINT);
            stream.addRect(LEFT, y - 120, CONTENT_W, 56);
            stream.fill();
            navyText("EXPERIENCIA", 8, LEFT + 16, y - 81, MUTED);
            navyText(xp + " XP", 26, LEFT + 16, y - 112, ORANGE);
            navyText("Rango · " + rank, 12, LEFT + 170, y - 106, NAVY);
            float ratio = Math.min(1f, Math.max(0f, xp / 3000f));
            float barW = CONTENT_W - 32;
            stream.setNonStrokingColor(LINE);
            stream.addRect(LEFT + 16, y - 132, barW, 5);
            stream.fill();
            if (ratio > 0.001) {
                stream.setNonStrokingColor(ORANGE);
                stream.addRect(LEFT + 16, y - 132, barW * ratio, 5);
                stream.fill();
            }
            navyText(Math.round(Math.min(1f, xp / 3000f) * 100) + "% hacia Mentor (3000 XP)", 8, LEFT + 16, y - 143, MUTED);
            y = y - 170;
        }

        void section(String title) throws IOException {
            if (y < 130) page();
            navyText(title.toUpperCase(java.util.Locale.ROOT), 12, LEFT, y - 4, NAVY);
            stream.setNonStrokingColor(CYAN);
            stream.addRect(LEFT, y - 8, 64, 2.2f);
            stream.fill();
            y -= 24;
        }

        void missionRow(com.laboratorio.mission.MissionService.MyMission mission, String averageFmt) throws IOException {
            if (y < 96) page();
            float rowH = 46;
            stream.setNonStrokingColor(FAINT);
            stream.addRect(LEFT, y - rowH, CONTENT_W, rowH);
            stream.fill();
            navyText(truncate(mission.title(), 46), 11, LEFT + 12, y - 17, TEXT);
            greyText((mission.mode().equals("GRUPAL") ? "GRUPAL" : mission.mode().equals("INTERDISCIPLINARIA") ? "INTERDISCIPLINARIA" : "INDIVIDUAL"), 8, LEFT + 12, y - 33, MUTED);
            float right = LEFT + CONTENT_W - 12;
            if (averageFmt == null) {
                greyText("Sin nota válida", 10, right - width("Sin nota válida", 10), y - 20, MUTED);
            } else {
                navyText(averageFmt, 12, right - width(averageFmt, 12), y - 15, NAVY);
                greyText("/ 5,0", 8, right - width("/ 5,0", 8), y - 29, MUTED);
                if (mission.invalidGrades() > 0) greyText("notas fuera de escala", 7, right - width("notas fuera de escala", 7), y - 40, ORANGE);
            }
            y -= rowH + 6;
        }

        void competencyRow(com.laboratorio.progress.ProgressService.Competency c, String averageFmt) throws IOException {
            if (y < 96) page();
            float rowH = 42;
            stream.setNonStrokingColor(FAINT);
            stream.addRect(LEFT, y - rowH, CONTENT_W, rowH);
            stream.fill();
            navyText(truncate(c.name(), 52), 10, LEFT + 12, y - 15, TEXT);
            greyText(c.track(), 8, LEFT + 12, y - 29, MUTED);
            float right = LEFT + CONTENT_W - 12;
            navyText(averageFmt, 12, right - width(averageFmt, 12), y - 15, NAVY);
            greyText("/ 5,0 · " + c.count() + " crit.", 8, right - width("/ 5,0 · " + c.count() + " crit.", 8), y - 29, MUTED);
            if (c.average().compareTo(BigDecimal.valueOf(3)) < 0) navyText("POR FORTALECER", 7, LEFT + 12, y - rowH + 9, ORANGE);
            y -= rowH + 6;
        }

        void badgeChip(String name, String description) throws IOException {
            if (y < 96) page();
            float chipW = Math.min(CONTENT_W - 10, width(name, 11) + 28);
            stream.setNonStrokingColor(new java.awt.Color(255, 244, 224));
            stream.addRect(LEFT, y - 24, chipW, 22);
            stream.fill();
            stream.setNonStrokingColor(ORANGE);
            stream.addRect(LEFT, y - 24, 3.5f, 22);
            stream.fill();
            navyText(name, 11, LEFT + 14, y - 10, NAVY);
            greyText(truncate(description, 74), 8, LEFT + 14, y - 22, MUTED);
            y -= 34;
        }

        void emptyNote(String text) throws IOException {
            greyText("· " + text, 10, LEFT + 10, y - 12, MUTED);
            y -= 26;
        }

        void note(String text) throws IOException {
            if (y < 80) page();
            greyText(text, 8, LEFT, y - 12, MUTED);
            y -= 20;
        }

        private String truncate(String text, int maxChars) {
            if (text == null) return "";
            String t = text.replaceAll("\\s+", " ").trim();
            return t.length() <= maxChars ? t : t.substring(0, maxChars - 1) + "…";
        }

        @Override public void close() throws IOException { if (stream != null) stream.close(); }
    }
}
