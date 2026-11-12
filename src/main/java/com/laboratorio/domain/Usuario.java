package com.laboratorio.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Identidad del dominio. La tabla {@code usuario} se mapea a esta jerarquía sellada:
 * {@link Estudiante}, {@link Profesor} y {@link Administrador}. Las cuentas multi-rol
 * se resuelven con {@link #prioridadAlta()} (ADMINISTRADOR &gt; PROFESOR &gt; ESTUDIANTE).
 */
public sealed abstract class Usuario permits Estudiante, Profesor, Administrador {
    private final UUID id;
    private final UUID academiaId;
    private final String correo;
    private final String nombre;
    private final String apellido;
    private final EstadoUsuario estado;
    private final Set<Rol> roles;

    protected Usuario(UUID id, UUID academiaId, String correo, String nombre, String apellido, EstadoUsuario estado, Set<Rol> roles) {
        if (id == null || academiaId == null) throw new IllegalArgumentException("La identidad requiere identificadores.");
        if (correo == null || !correo.equals(correo.strip().toLowerCase(java.util.Locale.ROOT)))
            throw new IllegalArgumentException("El correo debe estar normalizado en minúsculas.");
        if (nombre == null || nombre.isBlank() || apellido == null || apellido.isBlank())
            throw new IllegalArgumentException("Nombre y apellido son obligatorios.");
        this.id = id;
        this.academiaId = academiaId;
        this.correo = correo;
        this.nombre = nombre.strip();
        this.apellido = apellido.strip();
        this.estado = estado;
        this.roles = Set.copyOf(roles);
    }

    public UUID id() { return id; }
    public UUID academiaId() { return academiaId; }
    public String correo() { return correo; }
    public String nombre() { return nombre; }
    public String apellido() { return apellido; }
    public String nombreCompleto() { return nombre + " " + apellido; }
    public EstadoUsuario estado() { return estado; }
    public Set<Rol> roles() { return roles; }
    public boolean esEstudiante() { return roles.contains(Rol.ESTUDIANTE); }
    public boolean esProfesor() { return roles.contains(Rol.PROFESOR); }
    public boolean esAdministrador() { return roles.contains(Rol.ADMINISTRADOR); }

    /** Rol de mayor privilegio, útil para vistas que muestran un único distintivo. */
    public Rol prioridadAlta() {
        if (esAdministrador()) return Rol.ADMINISTRADOR;
        if (esProfesor()) return Rol.PROFESOR;
        return Rol.ESTUDIANTE;
    }

    /** Reconstruye el subtipo correcto desde una fila de la tabla {@code usuario}. */
    public static Usuario desde(UUID id, UUID academiaId, String correo, String nombre, String apellido, String estado, List<String> roles) {
        EstadoUsuario estadoEnum = EstadoUsuario.valueOf(estado.strip().toUpperCase(java.util.Locale.ROOT));
        Set<Rol> parsed = Rol.desdeCodigos(roles);
        Rol principal = parsed.contains(Rol.ADMINISTRADOR) ? Rol.ADMINISTRADOR
                : parsed.contains(Rol.PROFESOR) ? Rol.PROFESOR : Rol.ESTUDIANTE;
        return switch (principal) {
            case ADMINISTRADOR -> new Administrador(id, academiaId, correo, nombre, apellido, estadoEnum, parsed);
            case PROFESOR -> new Profesor(id, academiaId, correo, nombre, apellido, estadoEnum, parsed);
            case ESTUDIANTE -> new Estudiante(id, academiaId, correo, nombre, apellido, estadoEnum, parsed);
        };
    }

    public enum EstadoUsuario { ACTIVO, INACTIVO, BLOQUEADO }
}
