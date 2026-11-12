package com.laboratorio.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Manejo global de errores. En /api/** devuelve JSON coherente;
 * en el resto prefiere la vista amigable `error.html` (nunca Whitelabel/error 500 crudo).
 */
@ControllerAdvice(basePackages = "com.laboratorio")
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView responseStatus(ResponseStatusException ex, HttpServletRequest req, HttpServletResponse res, Model model) {
        return handle(ex, ex.getStatusCode().value(), req, res, model);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView illegalArgument(IllegalArgumentException ex, HttpServletRequest req, HttpServletResponse res, Model model) {
        return handle(ex, 400, req, res, model);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView illegalState(IllegalStateException ex, HttpServletRequest req, HttpServletResponse res, Model model) {
        return handle(ex, 409, req, res, model);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ModelAndView constraintViolation(jakarta.validation.ConstraintViolationException ex, HttpServletRequest req, HttpServletResponse res, Model model) {
        return handle(ex, 400, req, res, model);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unhandled(Exception ex, HttpServletRequest req, HttpServletResponse res, Model model) {
        return handle(ex, 500, req, res, model);
    }

    private ModelAndView handle(Exception ex, int status, HttpServletRequest req, HttpServletResponse res, Model model) {
        String errorId = UUID.randomUUID().toString();
        String path = req.getRequestURI();
        boolean wantsJson = path.startsWith("/api/")
                || req.getHeader("Accept") != null && req.getHeader("Accept").contains(MediaType.APPLICATION_JSON_VALUE);
        if (wantsJson) {
            res.setStatus(status);
            try {
                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                res.getWriter().write("{\"error\":{\"id\":\"" + errorId + "\",\"status\":" + status + ",\"message\":\"" + escape(ex.getMessage() == null ? "Error interno" : ex.getMessage()) + "\",\"path\":\"" + escape(path) + "\",\"timestamp\":\"" + Instant.now() + "\"}}");
            } catch (IOException ignored) { /* respuesta sin cuerpo si ya fue enviada */ }
            return new ModelAndView();
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.valueOf(status));
        mav.addObject("errorId", errorId);
        mav.addObject("status", status);
        return mav;
    }

    private static String escape(String in) {
        if (in == null) return "";
        return in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
