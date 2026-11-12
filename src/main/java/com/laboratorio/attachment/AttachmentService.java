package com.laboratorio.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentService {
    private static final long MAX=10L*1024*1024;
    private final JdbcTemplate db;
    private final Path root;
    public AttachmentService(JdbcTemplate db,@Value("${laboratorio.attachments.directory:data/adjuntos}") String directory) { this.db=db; this.root=Path.of(directory).toAbsolutePath().normalize(); }
    public void validate(MultipartFile file) {
        if(file==null || file.isEmpty()) return;
        String name=file.getOriginalFilename();
        if(name==null || name.isBlank() || name.length()>255 || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Nombre de archivo no válido.");
        if(file.getSize()>MAX) throw new IllegalArgumentException("El adjunto supera el límite de 10 MB.");
        try(InputStream input=file.getInputStream()) {
            if(!allowed(name.toLowerCase(Locale.ROOT),input.readNBytes(12))) throw new IllegalArgumentException("Adjunta un PDF, una imagen PNG, JPG, GIF o WebP, o un ZIP válido.");
        } catch(IOException ex) { throw new IllegalStateException("No se pudo leer el adjunto.",ex); }
    }
    /** Invocado dentro de la transacción de la entrega. */
    public void save(UUID version,MultipartFile file) {
        if(file==null || file.isEmpty()) return;
        String name=file.getOriginalFilename();
        if(name==null || name.isBlank() || name.length()>255 || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Nombre de archivo no válido.");
        if(file.getSize()>MAX) throw new IllegalArgumentException("El adjunto supera el límite de 10 MB.");
        String lower=name.toLowerCase(Locale.ROOT);
        try(InputStream input=file.getInputStream()) {
            byte[] header=input.readNBytes(12);
            if(!allowed(lower,header)) throw new IllegalArgumentException("Adjunta un PDF, una imagen PNG, JPG, GIF o WebP, o un ZIP válido.");
            try { Files.createDirectories(root,java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))); }
            catch(UnsupportedOperationException ex) { Files.createDirectories(root); }
            if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)) throw new IllegalStateException("El directorio de adjuntos no es válido.");
            try { Files.setPosixFilePermissions(root,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")); } catch(UnsupportedOperationException ignored) { }
            UUID key=UUID.randomUUID(); Path path=root.resolve(key.toString());
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            boolean complete=false;
            boolean created=false;
            try {
                try { Files.createFile(path,java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))); }
                catch(UnsupportedOperationException ex) { Files.createFile(path); }
                created=true;
                try(var output=Files.newOutputStream(path,StandardOpenOption.WRITE)) {
                    output.write(header); digest.update(header); long total=header.length; byte[] buffer=new byte[8192]; int n;
                    while((n=input.read(buffer))!=-1) { total+=n; if(total>MAX) throw new IllegalArgumentException("El adjunto supera el límite de 10 MB."); output.write(buffer,0,n); digest.update(buffer,0,n); }
                    if(total!=file.getSize()) throw new IllegalArgumentException("El tamaño del adjunto no coincide con lo recibido.");
                }
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) try { Files.deleteIfExists(path); } catch(IOException ignored) { } }
                });
                db.update("INSERT INTO adjunto_entrega(id,version_id,nombre_original,clave_almacenamiento,tipo_mime,tamano,hash) VALUES (?,?,?,?,?,?,?)",UUID.randomUUID(),version,name,key.toString(),"application/octet-stream",file.getSize(),java.util.HexFormat.of().formatHex(digest.digest()));
                complete=true;
            } finally { if(created && !complete) Files.deleteIfExists(path); }
        } catch(IOException | NoSuchAlgorithmException ex) { throw new IllegalStateException("No se pudo guardar el adjunto localmente.",ex); }
    }
    private boolean allowed(String name,byte[] h) {
        return name.endsWith(".pdf") && starts(h,0x25,0x50,0x44,0x46,0x2d)
            || name.endsWith(".png") && starts(h,0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a)
            || (name.endsWith(".jpg") || name.endsWith(".jpeg")) && starts(h,0xff,0xd8,0xff)
            || name.endsWith(".gif") && (starts(h,0x47,0x49,0x46,0x38,0x37,0x61) || starts(h,0x47,0x49,0x46,0x38,0x39,0x61))
            || name.endsWith(".webp") && h.length>=12 && starts(h,0x52,0x49,0x46,0x46) && h[8]=='W' && h[9]=='E' && h[10]=='B' && h[11]=='P'
            || name.endsWith(".zip") && (starts(h,0x50,0x4b,0x03,0x04) || starts(h,0x50,0x4b,0x05,0x06));
    }
    private boolean starts(byte[] bytes,int... signature) { if(bytes.length<signature.length) return false; for(int i=0;i<signature.length;i++) if((bytes[i]&255)!=signature[i]) return false; return true; }
    public Download find(String email,UUID id) {
        var rows=db.query("""
          SELECT DISTINCT a.nombre_original,a.clave_almacenamiento,a.tamano FROM adjunto_entrega a
          JOIN version_entrega v ON v.id=a.version_id JOIN entrega_etapa d ON d.id=v.entrega_etapa_id
          JOIN asignacion_mision am ON am.id=d.asignacion_id JOIN mision m ON m.id=d.mision_id
          JOIN mision_semillero ms ON ms.mision_id=m.id AND ms.semillero_id=am.semillero_destino_id JOIN usuario u ON u.correo=? AND u.estado='ACTIVO' AND u.academia_id=m.academia_id
          WHERE a.id=? AND (EXISTS (SELECT 1 FROM participante_asignacion pa WHERE pa.asignacion_id=am.id AND pa.usuario_id=u.id AND pa.retirado_en IS NULL)
           OR EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='ADMINISTRADOR')
           OR (EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo='PROFESOR')
             AND EXISTS (SELECT 1 FROM asignacion_semillero x JOIN asignacion_activa xa ON xa.asignacion_id=x.id WHERE x.usuario_id=u.id AND x.semillero_id=ms.semillero_id)))
          """,(rs,i)->new Download(rs.getString(1),rs.getString(2),rs.getLong(3)),email,id);
        if(rows.size()!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Download result=rows.getFirst();
        try { UUID.fromString(result.key()); } catch(IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        if(!Files.isRegularFile(root.resolve(result.key()),LinkOption.NOFOLLOW_LINKS)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return result;
    }
    public Path path(Download download) { return root.resolve(download.key()); }
    public record Download(String name,String key,long size) {}
}
