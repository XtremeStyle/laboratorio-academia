package com.laboratorio.gamification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BadgeService {
    private final JdbcTemplate db;
    private final RankingService ranking;
    public BadgeService(JdbcTemplate db,RankingService ranking) { this.db=db; this.ranking=ranking; }
    /** Las insignias y sus eventos se escriben en la transacción de evaluación. */
    public void reconcile(UUID assignment) {
        var people=db.query("SELECT u.id,u.correo FROM participante_asignacion pa JOIN usuario u ON u.id=pa.usuario_id WHERE pa.asignacion_id=? ORDER BY u.id",(rs,i)->new Person(rs.getObject(1,UUID.class),rs.getString(2)),assignment);
        for(Person person:people) {
            reconcilePerson(person);
        }
    }
    private void reconcilePerson(Person person) {
        db.query("SELECT id FROM usuario WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),person.id());
        var progress=ranking.personal(person.email());
        update(person.id(),"PRIMERA_MISION",progress.missions()>=1,false);
        update(person.id(),"CINCO_MISIONES",progress.missions()>=5,false);
        update(person.id(),"NOTA_PERFECTA",progress.perfect(),true);
    }
    private void update(UUID user,String code,boolean earned,boolean reversible) {
        UUID badge=db.queryForObject("SELECT id FROM insignia WHERE codigo=? AND estado='ACTIVA'",UUID.class,code);
        var current=db.query("SELECT id,revocada_en FROM insignia_usuario WHERE usuario_id=? AND insignia_id=?",(rs,i)->new Award(rs.getObject(1,UUID.class),rs.getObject(2,OffsetDateTime.class)),user,badge);
        if(current.isEmpty() && earned) {
            UUID id=UUID.randomUUID();
            db.update("INSERT INTO insignia_usuario(id,insignia_id,usuario_id,concedida_en,motivo) VALUES (?,?,?,CURRENT_TIMESTAMP,?)",id,badge,user,code);
            event(id,"CONCEDIDA");
        } else if(!current.isEmpty()) {
            Award award=current.getFirst();
            if(earned && award.revoked()!=null) { db.update("UPDATE insignia_usuario SET revocada_en=NULL WHERE id=?",award.id()); event(award.id(),"REACTIVADA"); }
            else if(!earned && reversible && award.revoked()==null) { db.update("UPDATE insignia_usuario SET revocada_en=CURRENT_TIMESTAMP WHERE id=?",award.id()); event(award.id(),"REVOCADA"); }
        }
    }
    private void event(UUID award,String action) { db.update("INSERT INTO evento_insignia(id,insignia_usuario_id,accion,ocurrido_en) VALUES (?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),award,action); }
    @Transactional
    public List<Badge> mine(String email) {
        var people=db.query("SELECT id,correo FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->new Person(rs.getObject(1,UUID.class),rs.getString(2)),email);
        if(people.size()==1) reconcilePerson(people.getFirst());
        return db.query("SELECT i.nombre,i.descripcion,iu.concedida_en FROM insignia_usuario iu JOIN insignia i ON i.id=iu.insignia_id JOIN usuario u ON u.id=iu.usuario_id WHERE u.correo=? AND u.estado='ACTIVO' AND iu.revocada_en IS NULL AND i.estado='ACTIVA' ORDER BY iu.concedida_en,i.nombre",(rs,i)->new Badge(rs.getString(1),rs.getString(2),rs.getObject(3,OffsetDateTime.class)),email);
    }
    private record Person(UUID id,String email) {}
    private record Award(UUID id,OffsetDateTime revoked) {}
    public record Badge(String name,String description,OffsetDateTime awarded) {}
}
