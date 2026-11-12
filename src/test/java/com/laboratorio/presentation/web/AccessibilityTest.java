package com.laboratorio.presentation.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessibilityTest {
    @Autowired MockMvc mvc;

    @Test void everyPublicPageExposesSkipLinkTargetContentId() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("skip-link")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"contenido\"")));
        mvc.perform(get("/registro")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("skip-link")));
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("skip-link")));
    }
}
