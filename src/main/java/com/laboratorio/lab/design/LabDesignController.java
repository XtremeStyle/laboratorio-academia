package com.laboratorio.lab.design;

import java.security.Principal;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/laboratorio/disenos")
public class LabDesignController {
    private final LabDesignService service;
    public LabDesignController(LabDesignService service) { this.service=service; }
    @GetMapping(value="/{type}",produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> load(Principal principal,@PathVariable String type) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(service.load(principal.getName(),type));
    }
    @PutMapping(value="/{type}",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> save(Principal principal,@PathVariable String type,@RequestBody JsonNode data) {
        service.save(principal.getName(),type,data); return ResponseEntity.noContent().build();
    }
}
