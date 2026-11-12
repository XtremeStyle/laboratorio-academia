package com.laboratorio.lab.design;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LabDesignIntegrationTest {
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Test @Transactional
    void savesAndRestoresOnlyTheOwnersValidDesign() throws Exception {
        UUID academy=UUID.randomUUID(),first=UUID.randomUUID(),second=UUID.randomUUID();
        String a="lab-"+first+"@test.invalid",b="lab-"+second+"@test.invalid";
        db.update("INSERT INTO academia(id,nombre,estado) VALUES (?,?,'ACTIVA')",academy,"Pruebas");
        for(var pair: java.util.List.of(new Object[]{first,a},new Object[]{second,b}))
            db.update("INSERT INTO usuario(id,academia_id,correo,nombre,apellido,password_hash,estado) VALUES (?,?,?,'Prueba','Laboratorio','hash','ACTIVO')",pair[0],academy,pair[1]);
        String design="{\"voltage\":5,\"parts\":[{\"a\":1,\"b\":0,\"r\":220}]}";
        mvc.perform(put("/laboratorio/disenos/CIRCUITO").with(user(a).roles("ESTUDIANTE")).contentType("application/json").content(design)).andExpect(status().isForbidden());
        mvc.perform(put("/laboratorio/disenos/CIRCUITO").with(user(a).roles("ESTUDIANTE")).with(csrf()).contentType("application/json").content(design)).andExpect(status().isNoContent());
        mvc.perform(get("/laboratorio/disenos/CIRCUITO").with(user(a).roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("220")));
        mvc.perform(get("/laboratorio/disenos/CIRCUITO").with(user(b).roles("ESTUDIANTE"))).andExpect(status().isNotFound());
        mvc.perform(put("/laboratorio/disenos/CIRCUITO").with(user(a).roles("ESTUDIANTE")).with(csrf()).contentType("application/json").content("{\"voltage\":5,\"parts\":[{\"a\":1,\"b\":1,\"r\":0}]}")) .andExpect(status().isBadRequest());
        mvc.perform(get("/laboratorio/disenos/CIRCUITO").with(user(a).roles("ESTUDIANTE"))).andExpect(content().string(containsString("220")));
        String rocket="{\"height\":25,\"fins\":3,\"nose\":\"cone\",\"stage\":2}";
        mvc.perform(put("/laboratorio/disenos/COHETE").with(user(b).roles("ESTUDIANTE")).with(csrf()).contentType("application/json").content(rocket)).andExpect(status().isNoContent());
        mvc.perform(get("/laboratorio/disenos/COHETE").with(user(b).roles("ESTUDIANTE"))).andExpect(status().isOk()).andExpect(content().string(containsString("cone")));
        mvc.perform(get("/laboratorio/disenos/COHETE")).andExpect(status().is3xxRedirection());
    }
}
