package com.laboratorio.chat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
    private final JdbcTemplate db;
    public ChatService(JdbcTemplate db) { this.db=db; }
    private Actor actor(String email) {
        var rows=db.query("SELECT id,academia_id,nombre,apellido FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->new Actor(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3)+" "+rs.getString(4)),email);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return rows.getFirst();
    }
    private boolean crewAllowed(Actor a,UUID crew) {
        Integer n=db.queryForObject("""
          SELECT COUNT(*) FROM tripulacion t JOIN semillero st ON st.id=t.semillero_id AND st.estado='ACTIVO' WHERE t.id=? AND t.academia_id=? AND t.estado='ACTIVA' AND
           (EXISTS (SELECT 1 FROM membresia_tripulacion mt JOIN membresia_activa ma ON ma.membresia_id=mt.id WHERE mt.tripulacion_id=t.id AND mt.usuario_id=?)
            OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='PROFESOR')
                AND EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=t.semillero_id)))
          """,Integer.class,crew,a.academy(),a.id(),a.id(),a.id());
        return n!=null && n>0;
    }
    private boolean pairAllowed(Actor a,UUID other) {
        Integer n=db.queryForObject("""
          SELECT COUNT(*) FROM usuario u JOIN asignacion_semillero x ON x.usuario_id=u.id JOIN asignacion_activa xa ON xa.asignacion_id=x.id
          JOIN semillero st ON st.id=x.semillero_id AND st.estado='ACTIVO'
          JOIN asignacion_semillero y ON y.semillero_id=x.semillero_id JOIN asignacion_activa ya ON ya.asignacion_id=y.id
          WHERE u.id=? AND u.academia_id=? AND u.estado='ACTIVO' AND y.usuario_id=? AND u.id<>? AND
          ((EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ESTUDIANTE')
            AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='PROFESOR'))
           OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='PROFESOR')
            AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE')))
          """,Integer.class,other,a.academy(),a.id(),a.id(),a.id(),a.id());
        return n!=null && n>0;
    }
    @Transactional(readOnly=true)
    public View view(String email) {
        Actor a=actor(email);
        var crews=db.query("""
          SELECT t.id,t.nombre FROM tripulacion t JOIN semillero st ON st.id=t.semillero_id AND st.estado='ACTIVO'
          WHERE t.academia_id=? AND t.estado='ACTIVA' AND
           (EXISTS (SELECT 1 FROM membresia_tripulacion mt JOIN membresia_activa ma ON ma.membresia_id=mt.id WHERE mt.tripulacion_id=t.id AND mt.usuario_id=?)
            OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='PROFESOR')
                AND EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=t.semillero_id)))
          ORDER BY t.nombre LIMIT 500
          """,(rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)),a.academy(),a.id(),a.id(),a.id());
        var peers=db.query("""
          SELECT DISTINCT u.id,u.nombre,u.apellido FROM usuario u JOIN asignacion_semillero x ON x.usuario_id=u.id JOIN asignacion_activa xa ON xa.asignacion_id=x.id
          JOIN semillero st ON st.id=x.semillero_id AND st.estado='ACTIVO'
          JOIN asignacion_semillero y ON y.semillero_id=x.semillero_id JOIN asignacion_activa ya ON ya.asignacion_id=y.id
          WHERE u.academia_id=? AND u.estado='ACTIVO' AND y.usuario_id=? AND u.id<>? AND
          ((EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ESTUDIANTE') AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='PROFESOR'))
            OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='PROFESOR') AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE')))
          ORDER BY u.apellido,u.nombre,u.id LIMIT 500
          """,(rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)+" "+rs.getString(3)),a.academy(),a.id(),a.id(),a.id(),a.id());
        var crewRooms=db.query("""
          SELECT c.conversacion_id,'Tripulación · ' || t.nombre FROM chat_tripulacion c JOIN tripulacion t ON t.id=c.tripulacion_id
          JOIN semillero st ON st.id=t.semillero_id AND st.estado='ACTIVO'
          WHERE c.academia_id=? AND t.estado='ACTIVA' AND
           (EXISTS (SELECT 1 FROM membresia_tripulacion mt JOIN membresia_activa ma ON ma.membresia_id=mt.id WHERE mt.tripulacion_id=t.id AND mt.usuario_id=?)
            OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='PROFESOR') AND EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=? AND x.semillero_id=t.semillero_id)))
          ORDER BY t.nombre LIMIT 500
          """,(rs,i)->new Room(rs.getObject(1,UUID.class),rs.getString(2)),a.academy(),a.id(),a.id(),a.id());
        var privateRooms=db.query("""
          SELECT DISTINCT cp.conversacion_id,'Privado · ' || u.nombre || ' ' || u.apellido FROM chat_privado cp
          JOIN usuario u ON u.id=CASE WHEN cp.usuario_1_id=? THEN cp.usuario_2_id ELSE cp.usuario_1_id END AND u.estado='ACTIVO'
          JOIN asignacion_semillero x ON x.usuario_id=u.id JOIN asignacion_activa xa ON xa.asignacion_id=x.id
          JOIN semillero st ON st.id=x.semillero_id AND st.estado='ACTIVO'
          JOIN asignacion_semillero y ON y.semillero_id=x.semillero_id JOIN asignacion_activa ya ON ya.asignacion_id=y.id
          WHERE cp.academia_id=? AND (cp.usuario_1_id=? OR cp.usuario_2_id=?) AND y.usuario_id=? AND
          ((EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='ESTUDIANTE') AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='PROFESOR'))
            OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=? AND r.codigo='PROFESOR') AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ESTUDIANTE')))
          ORDER BY 2 LIMIT 500
          """,(rs,i)->new Room(rs.getObject(1,UUID.class),rs.getString(2)),a.id(),a.academy(),a.id(),a.id(),a.id(),a.id(),a.id());
        List<Room> available=new ArrayList<>(crewRooms); available.addAll(privateRooms); available.sort(java.util.Comparator.comparing(Room::name));
        if(available.size()>500) available=new ArrayList<>(available.subList(0,500));
        return new View(crews,peers,available);
    }
    @Transactional
    public UUID crewRoom(String email,UUID crew) {
        Actor a=actor(email);
        db.query("SELECT id FROM tripulacion WHERE id=? AND academia_id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),crew,a.academy());
        if(!crewAllowed(a,crew)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var rooms=db.query("SELECT conversacion_id FROM chat_tripulacion WHERE tripulacion_id=?",(rs,i)->rs.getObject(1,UUID.class),crew);
        if(!rooms.isEmpty()) return rooms.getFirst();
        UUID room=UUID.randomUUID(); db.update("INSERT INTO conversacion(id,academia_id,tipo,creada_en) VALUES (?,?,'TRIPULACION',CURRENT_TIMESTAMP)",room,a.academy());
        db.update("INSERT INTO chat_tripulacion(tripulacion_id,academia_id,conversacion_id) VALUES (?,?,?)",crew,a.academy(),room);
        return room;
    }
    @Transactional
    public UUID privateRoom(String email,UUID other) {
        Actor a=actor(email);
        var ids=java.util.stream.Stream.of(a.id(),other).distinct().sorted().toList();
        if(ids.size()!=2) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        for(UUID id:ids) db.query("SELECT id FROM usuario WHERE id=? AND academia_id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),id,a.academy());
        if(!pairAllowed(a,other)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var rooms=db.query("SELECT conversacion_id FROM chat_privado WHERE academia_id=? AND usuario_1_id=? AND usuario_2_id=?",(rs,i)->rs.getObject(1,UUID.class),a.academy(),ids.get(0),ids.get(1));
        if(!rooms.isEmpty()) return rooms.getFirst();
        UUID room=UUID.randomUUID(); db.update("INSERT INTO conversacion(id,academia_id,tipo,creada_en) VALUES (?,?,'PRIVADA',CURRENT_TIMESTAMP)",room,a.academy());
        db.update("INSERT INTO chat_privado(id,academia_id,conversacion_id,usuario_1_id,usuario_2_id) VALUES (?,?,?,?,?)",UUID.randomUUID(),a.academy(),room,ids.get(0),ids.get(1));
        return room;
    }
    private Room allowed(Actor actor,UUID room) {
        var crews=db.query("SELECT ct.tripulacion_id,t.nombre FROM chat_tripulacion ct JOIN tripulacion t ON t.id=ct.tripulacion_id WHERE ct.conversacion_id=? AND ct.academia_id=?",(rs,i)->new Option(rs.getObject(1,UUID.class),rs.getString(2)),room,actor.academy());
        if(!crews.isEmpty() && crewAllowed(actor,crews.getFirst().id())) return new Room(room,"Tripulación · "+crews.getFirst().name());
        var peers=db.query("SELECT usuario_1_id,usuario_2_id FROM chat_privado WHERE conversacion_id=? AND academia_id=?",(rs,i)->new Pair(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class)),room,actor.academy());
        if(peers.isEmpty()) return null;
        Pair pair=peers.getFirst();
        UUID other=pair.one().equals(actor.id()) ? pair.two() : pair.two().equals(actor.id()) ? pair.one() : null;
        if(other==null || !pairAllowed(actor,other)) return null;
        String name=db.queryForObject("SELECT nombre || ' ' || apellido FROM usuario WHERE id=?",String.class,other);
        return new Room(room,"Privado · "+name);
    }
    @Transactional(readOnly=true)
    public Room room(String email,UUID id) { Room room=allowed(actor(email),id); if(room==null) throw new ResponseStatusException(HttpStatus.FORBIDDEN); return room; }
    @Transactional(readOnly=true)
    public List<Message> messages(String email,UUID id) { return messagesPage(email,id,0).items(); }
    @Transactional(readOnly=true)
    public MessagePage messagesPage(String email,UUID id,int page) {
        if(page<0 || page>20000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Página no válida.");
        room(email,id);
        var rows=db.query("SELECT m.id,m.contenido,m.enviado_en,u.nombre,u.apellido FROM mensaje m JOIN usuario u ON u.id=m.autor_id WHERE m.conversacion_id=? AND m.eliminado_en IS NULL ORDER BY m.enviado_en DESC,m.id DESC LIMIT 101 OFFSET ?",(rs,i)->new Message(rs.getObject(1,UUID.class),rs.getString(4)+" "+rs.getString(5),rs.getString(2),rs.getObject(3,OffsetDateTime.class).toString()),id,page*100);
        boolean more=rows.size()>100;
        var pageRows=new ArrayList<>(rows.subList(0,Math.min(rows.size(),100))); java.util.Collections.reverse(pageRows);
        return new MessagePage(List.copyOf(pageRows),more,page);
    }
    @Transactional
    public Message send(String email,UUID room,String content) {
        if(content==null || content.isBlank() || content.strip().length()>2000) throw new IllegalArgumentException("El mensaje debe tener entre 1 y 2000 caracteres.");
        Actor a=actor(email);
        // Control de abuso: rechaza mensajes idénticos reenviados en una ventana de 10 s.
        Integer duplicates=db.queryForObject("SELECT COUNT(*) FROM mensaje WHERE conversacion_id=? AND autor_id=? AND contenido=? AND enviado_en>=?",Integer.class,room,a.id(),content.strip(),OffsetDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(10));
        if(duplicates!=null && duplicates>0) throw new IllegalArgumentException("Mensaje repetido. Espera unos segundos.");
        db.query("SELECT id FROM conversacion WHERE id=? AND academia_id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),room,a.academy());
        if(allowed(a,room)==null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Integer exists=db.queryForObject("SELECT COUNT(*) FROM participante_conversacion WHERE conversacion_id=? AND usuario_id=?",Integer.class,room,a.id());
        if(exists==null || exists==0) db.update("INSERT INTO participante_conversacion(id,academia_id,conversacion_id,usuario_id,unido_en) VALUES (?,?,?,?,CURRENT_TIMESTAMP)",UUID.randomUUID(),a.academy(),room,a.id());
        UUID id=UUID.randomUUID(); OffsetDateTime sent=OffsetDateTime.now(java.time.ZoneOffset.UTC);
        db.update("INSERT INTO mensaje(id,academia_id,conversacion_id,autor_id,contenido,enviado_en) VALUES (?,?,?,?,?,?)",id,a.academy(),room,a.id(),content.strip(),sent);
        return new Message(id,a.name(),content.strip(),sent.toString());
    }
    private record Actor(UUID id,UUID academy,String name) {}
    private record Pair(UUID one,UUID two) {}
    public record Option(UUID id,String name) {}
    public record Room(UUID id,String name) {}
    public record Message(UUID id,String author,String content,String sent) {}
    public record MessagePage(List<Message> items,boolean more,int number) {}
    public record View(List<Option> crews,List<Option> peers,List<Room> rooms) {}
}
