package controllers;

import models.Usuario;
import models.Rol;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.time.LocalDateTime;

/**
 * Controlador que gestiona todas las operaciones de base de datos
 * relacionadas con la entidad Usuario.
 *
 * Además del CRUD básico, gestiona la autenticación (login) y el
 * restablecimiento de contraseñas. Las contraseñas nunca se almacenan
 * en texto plano: se aplica BCrypt antes de guardarlas.
 *
 * BCrypt es el estándar actual para el hash de contraseñas porque:
 * - Incorpora un salt aleatorio automáticamente en cada hash.
 * - Es resistente a ataques de fuerza bruta por su coste computacional
 * configurable.
 * - A diferencia de SHA-256, dos hashes del mismo texto plano son siempre
 * diferentes.
 *
 * Hereda de BaseController para reutilizar la lógica de conexión
 * y el logger compartido.
 */
public class UsuarioController extends BaseController {

    /**
     * Verifica las credenciales del usuario e inicia sesión si son correctas.
     *
     * Buscamos el usuario solo por email y luego verificamos la contraseña
     * en Java con BCrypt. No comparamos el hash en SQL porque BCrypt genera
     * un salt diferente en cada hash y la comparación directa no funciona.
     *
     * @param email    correo electrónico introducido en el formulario de login
     * @param password contraseña en texto plano introducida en el formulario
     * @return el usuario autenticado con su rol, o null si las credenciales son
     *         incorrectas
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Usuario login(String email, String password) {

        String sql = "SELECT u.*, r.nombre as rol_nombre, r.descripcion as rol_descripcion " +
                "FROM Usuario u " +
                "JOIN Rol r ON u.id_rol = r.id_rol " +
                "WHERE u.email = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashAlmacenado = rs.getString("contrasena");

                    // Verificamos la contraseña con BCrypt
                    if (!verificarPassword(password, hashAlmacenado)) {
                        return null;
                    }

                    // Credenciales correctas: registramos la fecha y hora del acceso
                    String updateLoginSql = "UPDATE Usuario SET last_login = ? WHERE id_usuario = ?";
                    try (PreparedStatement updateLoginStmt = conn.prepareStatement(updateLoginSql)) {
                        updateLoginStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                        updateLoginStmt.setInt(2, rs.getInt("id_usuario"));
                        updateLoginStmt.executeUpdate();
                    }
                    return mapUsuarioFromResultSet(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar sesión", e);
            throw new RuntimeException("Error de base de datos durante el inicio de sesión", e);
        }
    }

    /**
     * Inserta un nuevo usuario en la base de datos.
     *
     * La contraseña se hashea con BCrypt antes de almacenarla.
     *
     * @param usuario objeto con los datos del usuario a crear
     * @return el mismo objeto usuario con el ID asignado
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Usuario create(Usuario usuario) {

        String sql = "INSERT INTO Usuario (nombre, email, contrasena, id_rol, last_login) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, hashPassword(usuario.getPassword()));
            stmt.setInt(4, usuario.getRol().getIdRol());
            stmt.setTimestamp(5, usuario.getLastLogin() != null ? Timestamp.valueOf(usuario.getLastLogin()) : null);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear el usuario: sin filas afectadas.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setIdUsuario(rs.getInt(1));
                    return usuario;
                } else {
                    throw new SQLException("Error al crear el usuario: no se obtuvo ID generado.");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear usuario", e);
            throw new RuntimeException("Error de la base de datos al crear el usuario", e);
        }
    }

    /**
     * Actualiza los datos de un usuario existente en la base de datos.
     *
     * Si el campo password no está vacío, también actualiza la contraseña
     * hasheándola con BCrypt antes de guardarla.
     *
     * @param usuario objeto con los datos actualizados (debe tener ID válido)
     * @return el mismo objeto usuario con los datos actualizados
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Usuario update(Usuario usuario) {

        String sql;
        boolean actualizarPassword = usuario.getPassword() != null && !usuario.getPassword().isEmpty();

        if (actualizarPassword) {
            sql = "UPDATE Usuario SET nombre = ?, email = ?, contrasena = ?, id_rol = ?, last_login = ? WHERE id_usuario = ?";
        } else {
            sql = "UPDATE Usuario SET nombre = ?, email = ?, id_rol = ?, last_login = ? WHERE id_usuario = ?";
        }

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());

            int indice = 3;
            if (actualizarPassword) {
                stmt.setString(indice++, hashPassword(usuario.getPassword()));
            }

            stmt.setInt(indice++, usuario.getRol().getIdRol());
            stmt.setTimestamp(indice++,
                    usuario.getLastLogin() != null ? Timestamp.valueOf(usuario.getLastLogin()) : null);
            stmt.setInt(indice, usuario.getIdUsuario());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el usuario: no hay filas afectadas.");
            }

            return usuario;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar usuario", e);
            throw new RuntimeException("Error de la base de datos al actualizar el usuario", e);
        }
    }

    /**
     * Elimina un usuario de la base de datos por su ID.
     *
     * @param id identificador del usuario a eliminar
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public void delete(Integer id) {

        String sql = "DELETE FROM Usuario WHERE id_usuario = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al eliminar el usuario: no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar usuario", e);
            throw new RuntimeException("Error de la base de datos al eliminar el usuario", e);
        }
    }

    /**
     * Busca y devuelve un usuario por su ID.
     *
     * @param id identificador del usuario a buscar
     * @return el usuario encontrado con su rol, o null si no existe
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Usuario findById(Integer id) {

        String sql = "SELECT u.*, r.nombre as rol_nombre, r.descripcion as rol_descripcion " +
                "FROM Usuario u " +
                "JOIN Rol r ON u.id_rol = r.id_rol " +
                "WHERE u.id_usuario = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUsuarioFromResultSet(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar usuario por ID", e);
            throw new RuntimeException("Error de la base de datos al buscar el usuario", e);
        }
    }

    /**
     * Devuelve la lista completa de usuarios registrados en el sistema.
     *
     * @return lista de usuarios (vacía si no hay ninguno)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Usuario> findAll() {

        String sql = "SELECT u.*, r.nombre as rol_nombre, r.descripcion as rol_descripcion " +
                "FROM Usuario u " +
                "JOIN Rol r ON u.id_rol = r.id_rol";

        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapUsuarioFromResultSet(rs));
            }

            return usuarios;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todos los usuarios", e);
            throw new RuntimeException("Error de la base de datos al obtener los usuarios", e);
        }
    }

    /**
     * Restablece la contraseña de un usuario hasheándola con BCrypt.
     *
     * @param usuarioId     identificador del usuario
     * @param nuevoPassword nueva contraseña en texto plano
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public void resetPassword(Integer usuarioId, String nuevoPassword) {

        String sql = "UPDATE Usuario SET contrasena = ? WHERE id_usuario = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, hashPassword(nuevoPassword));
            stmt.setInt(2, usuarioId);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al restablecer la contraseña: no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al restablecer la contraseña", e);
            throw new RuntimeException("Error de la base de datos al restablecer la contraseña", e);
        }
    }

    /**
     * Convierte una fila del ResultSet en un objeto Usuario con su Rol.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Usuario con todos sus datos y el rol asociado
     * @throws SQLException si alguna columna no existe o hay error de lectura
     */
    private Usuario mapUsuarioFromResultSet(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setEmail(rs.getString("email"));

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            usuario.setLastLogin(lastLogin.toLocalDateTime());
        }

        Rol rol = new Rol();
        rol.setIdRol(rs.getInt("id_rol"));
        rol.setNombre(rs.getString("rol_nombre"));
        rol.setDescripcion(rs.getString("rol_descripcion"));

        usuario.setRol(rol);
        return usuario;
    }

    /**
     * Genera el hash BCrypt de una contraseña en texto plano.
     *
     * BCrypt incorpora un salt aleatorio automáticamente en cada llamada,
     * lo que significa que el mismo texto plano produce hashes diferentes
     * en cada ejecución. Esto protege contra ataques de diccionario y
     * tablas rainbow, a diferencia de SHA-256 sin salt.
     *
     * El factor de coste (10) determina cuánto tiempo tarda el hash:
     * más alto = más seguro pero más lento. 10 es el valor estándar recomendado.
     *
     * @param password contraseña en texto plano
     * @return hash BCrypt de la contraseña
     */
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash BCrypt.
     *
     * No podemos comparar hashes directamente porque BCrypt genera un salt
     * diferente cada vez. BCrypt.checkpw extrae el salt del hash almacenado
     * y verifica la contraseña correctamente.
     *
     * @param password       contraseña en texto plano introducida por el usuario
     * @param hashedPassword hash BCrypt almacenado en la base de datos
     * @return true si la contraseña es correcta, false en caso contrario
     */
    private boolean verificarPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}