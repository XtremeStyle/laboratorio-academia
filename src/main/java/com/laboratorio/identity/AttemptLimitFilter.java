package com.laboratorio.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Freno local anti fuerza bruta: limita los envíos del formulario de login por IP.
 * 20 intentos en 10 minutos; en exceso responde 429 con Retry-After. Es intencionalmente
 * sin estado de BD ni IP persistente, y en memoria del proceso (una instancia local).
 */
@Component
public class AttemptLimitFilter extends OncePerRequestFilter {
    private static final long VENTANA_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_INTENTOS = 20;
    private static final int MAX_IP_REGISTRADAS = 10000;
    private final Map<String, ArrayDeque<Long>> intentos = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        String ip = clientKey(request);
        long now = Instant.now().toEpochMilli();
        if (intentos.size() >= MAX_IP_REGISTRADAS) limpiarViejas(now);
        ArrayDeque<Long> ventana = intentos.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (ventana) {
            while (!ventana.isEmpty() && now - ventana.peekFirst() > VENTANA_MILLIS) ventana.pollFirst();
            if (ventana.size() >= MAX_INTENTOS) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(VENTANA_MILLIS / 1000));
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().write("<!DOCTYPE html><title>Espera unos minutos</title><main><h1>Demasiados intentos</h1><p>Espera unos minutos antes de volver a intentarlo.</p></main>");
                return;
            }
            ventana.addLast(now);
        }
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null ? "desconocida" : ip;
    }

    private void limpiarViejas(long now) {
        for (Iterator<Map.Entry<String, ArrayDeque<Long>>> it = intentos.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ArrayDeque<Long>> entry = it.next();
            synchronized (entry.getValue()) {
                while (!entry.getValue().isEmpty() && now - entry.getValue().peekFirst() > VENTANA_MILLIS) entry.getValue().pollFirst();
            }
            if (entry.getValue().isEmpty()) it.remove();
        }
    }
}
