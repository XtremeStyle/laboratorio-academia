package com.laboratorio.attachment;
import java.security.Principal;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@Controller
public class AttachmentController {
    private final AttachmentService service;
    public AttachmentController(AttachmentService service) { this.service=service; }
    @GetMapping("/adjuntos/{id}")
    public ResponseEntity<FileSystemResource> download(Principal principal,@PathVariable UUID id) {
        var item=service.find(principal.getName(),id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(item.name(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(item.size()).body(new FileSystemResource(service.path(item)));
    }
}
