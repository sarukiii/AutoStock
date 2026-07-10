package models;

import java.util.Objects;

/**
 * Modelo que representa un rol de usuario en el sistema.
 *
 * El rol determina los permisos y el nivel de acceso de cada usuario.
 * Actualmente el sistema maneja dos roles:
 * - Administrador (id_rol = 1): acceso completo a todos los módulos.
 * - Empleado (id_rol = 2): acceso limitado a productos y ventas.
 *
 * Sus atributos se corresponden con las columnas de la tabla Rol en MySQL.
 */
public class Rol {

    // Identificador único del rol en la base de datos
    private Integer idRol;
    private String nombre;
    private String descripcion;

    // Constructor vacío necesario para que UsuarioController pueda
    // crear instancias al mapear los resultados de la base de datos
    public Rol() {
    }

    /**
     * Constructor con todos los campos excepto el ID.
     *
     * @param nombre      nombre del rol (ej: "Administrador", "Empleado")
     * @param descripcion descripción de los permisos asociados al rol
     */
    public Rol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que los controladores
    // puedan leer y escribir los atributos del modelo.

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Dos roles son iguales si tienen el mismo ID.
     * Necesario para comparar roles correctamente en colecciones.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Rol rol = (Rol) o;
        return Objects.equals(idRol, rol.idRol);
    }

    /**
     * El hashCode se basa en el ID para mantener consistencia con equals.
     * Si dos objetos son iguales según equals, deben tener el mismo hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idRol);
    }
}