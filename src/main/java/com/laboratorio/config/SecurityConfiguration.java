package com.laboratorio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http,org.springframework.jdbc.core.JdbcTemplate jdbc, com.laboratorio.identity.AttemptLimitFilter attemptLimitFilter) throws Exception {
        return http
                .addFilterBefore(attemptLimitFilter,org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new com.laboratorio.identity.SessionAccountFilter(jdbc),org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/registro",
                                "/demostracion/semilleros",
                                "/error",
                                "/actuator/health",
                                "/actuator/info",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/webjars/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers("/administracion/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/academia/**").hasAnyRole("ADMINISTRADOR", "PROFESOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.loginPage("/login").permitAll().successHandler((request,response,authentication) -> {
                    if(!(authentication.getPrincipal() instanceof com.laboratorio.identity.DatabaseUserDetailsService.SessionUser user)) { org.springframework.security.core.context.SecurityContextHolder.clearContext(); request.getSession().invalidate(); response.sendRedirect(request.getContextPath()+"/login?error"); return; }
                    request.getSession().setAttribute(com.laboratorio.identity.SessionAccountFilter.ACCOUNT_SNAPSHOT,user.authenticatedHash());
                    new org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(request,response,authentication);
                }))
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'; " +
                                "frame-ancestors 'none'; " +
                                "object-src 'none'; " +
                                "script-src 'self'; " +
                                "style-src 'self'; " +
                                "img-src 'self' data:; " +
                                "font-src 'self' data:; " +
                                "connect-src 'self' ws: wss:"))
                        .permissionsPolicy(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()"))
                )
                .logout(Customizer.withDefaults())
                .build();
    }
}
