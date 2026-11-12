package com.laboratorio.domain;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Roles persistidos en el catálogo `rol`. La comparación es insensible a mayúsculas. */
public enum Rol {
    ESTUDIANTE, PROFESOR, ADMINISTRADOR;

    public static Set<Rol> desdeCodigos(Collection<String> codigos) {
        if (codigos == null) return Set.of();
        return codigos.stream()
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .map(codigo -> Rol.valueOf(codigo.strip().toUpperCase(Locale.ROOT)))
                .collect(Collectors.toUnmodifiableSet());
    }
}
