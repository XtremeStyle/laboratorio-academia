package com.laboratorio.presentation.web;

import com.laboratorio.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@ActiveProfiles("test")
@Import(SecurityConfiguration.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private DashboardService dashboard;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.laboratorio.gamification.RankingService ranking;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.laboratorio.gamification.BadgeService badges;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void homePageIsPublicAndReturnsIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void otherRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/ruta-protegida"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/"))
                .andExpect(status().isForbidden());
    }
}
