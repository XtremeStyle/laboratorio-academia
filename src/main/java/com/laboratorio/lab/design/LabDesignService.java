package com.laboratorio.lab.design;

import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.laboratorio.audit.AuditService;

@Service
public class LabDesignService {
    private final JdbcTemplate db;
    private final AuditService audit;
    public LabDesignService(JdbcTemplate db, AuditService audit) { this.db=db; this.audit=audit; }
    private UUID actor(String email) {
        var users=db.query("SELECT id FROM usuario WHERE correo=? AND estado='ACTIVO'",(rs,i)->rs.getObject(1,UUID.class),email);
        if(users.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return users.getFirst();
    }
    private void kind(String type) {
        if(!"CIRCUITO".equals(type) && !"COHETE".equals(type)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    private boolean number(JsonNode node,String key,double min,double max) {
        JsonNode v=node.path(key); return v.isNumber() && Double.isFinite(v.doubleValue()) && v.doubleValue()>=min && v.doubleValue()<=max;
    }
    private void validate(String type,JsonNode data) {
        if(data==null || !data.isObject() || data.toString().length()>4096) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Diseño no válido.");
        if(type.equals("CIRCUITO")) {
            JsonNode parts=data.path("parts");
            if(!number(data,"voltage",0,24) || !parts.isArray() || parts.size()>8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Circuito no válido.");
            for(JsonNode part:parts) {
                if(!part.isObject() || !part.path("a").isInt() || !part.path("b").isInt() || !number(part,"a",0,3) || !number(part,"b",0,3) || part.path("a").asInt()==part.path("b").asInt() || !number(part,"r",1,1000000)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Resistencia no válida.");
            }
        } else if(!number(data,"height",15,35) || !data.path("height").isInt() || !number(data,"fins",3,4) || !data.path("fins").isInt() || !("cone".equals(data.path("nose").asText()) || "round".equals(data.path("nose").asText())) || !number(data,"stage",0,4) || !data.path("stage").isInt()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cohete no válido.");
    }
    @Transactional(readOnly=true)
    public String load(String email,String type) {
        kind(type);UUID user=actor(email);
        var found=db.query("SELECT contenido FROM diseno_laboratorio WHERE usuario_id=? AND tipo=?",(rs,i)->rs.getString(1),user,type);
        if(found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return found.getFirst();
    }
    @Transactional
    public void save(String email,String type,JsonNode data) {
        kind(type);validate(type,data);UUID user=actor(email);
        db.query("SELECT id FROM usuario WHERE id=? FOR UPDATE",(rs,i)->rs.getObject(1,UUID.class),user);
        int updated=db.update("UPDATE diseno_laboratorio SET contenido=?,actualizado_en=CURRENT_TIMESTAMP WHERE usuario_id=? AND tipo=?",data.toString(),user,type);
        UUID id;
        if(updated==0) { id=UUID.randomUUID(); db.update("INSERT INTO diseno_laboratorio(id,usuario_id,tipo,contenido) VALUES (?,?,?,?)",id,user,type,data.toString()); }
        else id=db.queryForObject("SELECT id FROM diseno_laboratorio WHERE usuario_id=? AND tipo=?",UUID.class,user,type);
        audit.record(user,"GUARDAR_DISENO","DISENO_LABORATORIO",id);
    }
}
