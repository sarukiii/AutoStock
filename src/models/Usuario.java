package models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo que representa un usuario del sistema.
 *
 * Un usuario es la persona que accede a la aplicación con sus credenciales.
 * Tiene un rol asociado que determina su nivel de acceso. Sus atributos
 * se corresponden con las columnas de la tabla Usuario en MySQL.
 *
 * Importante: el campo password nunca se almacena en texto plano.
 * UsuarioController aplica un hash SHA-256 antes de guardarlo en la BD.
 * En este modelo, password actúa como campo temporal para transportar
 * la contraseña desde el formulario hasta el controlador.
 */
public class Usuario {

    // Identificador único generado automáticamente por la base de datos
    private Integer idUsuario;
    private String nombre;
    private String email;
    // Campo temporal: se usa para recibir la contraseña del formulario.
    // Nunca se almacena en texto plano; UsuarioController lo hashea antes de
    // guardarlo.
    private String password;
    // Rol que determina los permisos del usuario (Administrador o Empleado)
    private Rol rol;
    // Fecha y hora del último acceso exitoso al sistema
    private LocalDateTime lastLogin;

    // Constructor vacío necesario para que UsuarioController pueda
    // crear instancias al mapear los resultados de la base de datos
    public Usuario() {
    }

    /**
     * Constructor con los campos necesarios para crear un nuevo usuario.
     * El ID lo asigna la base de datos y lastLogin será null hasta el primer
     * acceso.
     *
     * @param nombre   nombre completo del usuario
     * @param email    correo electrónico (usado como identificador de acceso)
     * @param password contraseña en texto plano (se hashea en el controlador)
     * @param rol      rol asignado al usuario
     */
    public Usuario(String nombre, String email, String password, Rol rol) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol = rol;
    }

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que los controladores
    // puedan leer y escribir los atributos del modelo.

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Dos usuarios son iguales si tienen el mismo ID.
     * Necesario para comparar usuarios correctamente en colecciones.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(idUsuario, usuario.idUsuario);
    }

    /**
     * El hashCode se basa en el ID para mantener consistencia con equals.
     * Si dos objetos son iguales según equals, deben tener el mismo hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idUsuario);
    }

    /**
     * Representación textual del usuario para depuración.
     * No incluye la contraseña para evitar exponerla en los logs.
     */
    @Override
    public String toString() {
        return "Usuario{" +
                "idUsuario=" + idUsuario +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", rol=" + rol +
                ", lastLogin=" + lastLogin +
                '}';
    }
}