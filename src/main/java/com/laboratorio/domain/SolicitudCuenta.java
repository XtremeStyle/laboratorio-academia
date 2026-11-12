package com.laboratorio.domain;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/** Solicitud pública de acceso, pendiente de aprobación administrativa. */
public record SolicitudCuenta(UUID id, UUID academiaId, String correo, String nombre, String apellido, Rol rolSolicitado, String mensaje, EstadoSolicitud estado, OffsetDateTime creadaEn) {
    public SolicitudCuenta {
        if (id == null || academiaId == null) throw new IllegalArgumentException("La solicitud requiere identificadores.");
        String correoNormalizado = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        if (correoNormalizado.length() > 254 || !correoNormalizado.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("El correo no es válido.");
        if (nombre == null || nombre.isBlank() || nombre.length() > 120 || apellido == null || apellido.isBlank() || apellido.length() > 120)
            throw new IllegalArgumentException("Nombre y apellido son obligatorios (máx. 120 caracteres).");
        if (mensaje != null && mensaje.length() > 500)
            throw new IllegalArgumentException("El mensaje admite hasta 500 caracteres.");
        correo = correoNormalizado;
        nombre = nombre.strip();
        apellido = apellido.strip();
        mensaje = mensaje == null ? null : mensaje.strip();
        if (rolSolicitado == null || rolSolicitado == Rol.ADMINISTRADOR)
            throw new IllegalArgumentException("Solo se pueden solicitar los roles ESTUDIANTE y PROFESOR.");
    }

    public enum EstadoSolicitud { PENDIENTE, APROBADA, RECHAZADA }
}
