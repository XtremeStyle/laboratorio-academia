package com.laboratorio.research;
import com.laboratorio.audit.AuditService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class SemilleroAdministration {
    private final JdbcTemplate db; private final AuditService audit;
    private final com.laboratorio.course.CourseCatalogSeeder catalog;
    public SemilleroAdministration(JdbcTemplate db,AuditService audit,com.laboratorio.course.CourseCatalogSeeder catalog) { this.db=db; this.audit=audit; this.catalog=catalog; }
    @Transactional
    public void create(String email,String code,String name,String description) {
        var actors=db.query("SELECT u.id,u.academia_id FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id WHERE u.correo=? AND u.estado='ACTIVO' AND r.codigo='ADMINISTRADOR'",(rs,i)->new Actor(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),email);
        if(actors.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        code=code.strip().toUpperCase(Locale.ROOT); name=name.strip(); description=description.strip();
        if(!code.matches("[A-Z0-9_-]{2,50}") || name.isEmpty() || name.length()>160 || description.length()>2000) throw new IllegalArgumentException("Usa un código de 2 a 50 letras sin tildes, números, guiones o guiones bajos; un nombre de hasta 160 caracteres y una descripción de hasta 2000.");
        UUID id=UUID.randomUUID(); var actor=actors.getFirst();
        db.update("INSERT INTO semillero(id,academia_id,codigo,nombre,descripcion,estado) VALUES (?,?,?,?,?,'ACTIVO')",id,actor.academy(),code,name,description);
        // El semillero nuevo recibe sus cursos asociados sin esperar al siguiente arranque.
        catalog.link(id);
        audit.record(actor.id(),"CREAR_SEMILLERO","SEMILLERO",id);
    }
    private record Actor(UUID id,UUID academy) {}
}
