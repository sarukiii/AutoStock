package controllers;

import models.Usuario;
import models.Rol;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.time.LocalDateTime;

/**
 * Controlador que gestiona todas las operaciones de base de datos
 * relacionadas con la entidad Usuario.
 *
 * Además del CRUD básico, gestiona la autenticación (login) y el
 * restablecimiento de contraseñas. Las contraseñas nunca se almacenan
 * en texto plano: se aplica un hash SHA-256 antes de guardarlas.
 *
 * Hereda de BaseController para reutilizar la lógica de conexión
 * y el logger compartido.
 */
public class UsuarioController extends BaseController {

    /**
     * Verifica las credenciales del usuario e inicia sesión si son correctas.
     *
     * Hashea la contraseña recibida y la compara con la almacenada en la base
     * de datos. Si coinciden, registra la fecha y hora del último acceso y
     * devuelve el objeto Usuario con su rol cargado.
     *
     * @param email    correo electrónico introducido en el formulario de login
     * @param password contraseña en texto plano introducida en el formulario
     * @return el usuario autenticado con su rol, o null si las credenciales son
     *         incorrectas
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Usuario login(String email, String password) {

        // JOIN con Rol para obtener los datos del rol del usuario en una sola consulta
        String sql = "SELECT u.*, r.nombre as rol_nombre, r.descripcion as rol_descripcion " +
                "FROM Usuario u " +
                "JOIN Rol r ON u.id_rol = r.id_rol " +
                "WHERE u.email = ? AND u.contrasena = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            // Hasheamos la contraseña antes de compararla con la almacenada en BD
            stmt.setString(2, hashPassword(password));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Credenciales correctas: registramos la fecha y hora del acceso
                    String updateLoginSql = "UPDATE Usuario SET last_login = ? WHERE id_usuario = ?";
                    try (PreparedStatement updateLoginStmt = conn.prepareStatement(updateLoginSql)) {
                        updateLoginStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                        updateLoginStmt.setInt(2, rs.getInt("id_usuario"));
                        updateLoginStmt.executeUpdate();
                    }
                    return mapUsuarioFromResultSet(rs);
                }
                // Credenciales incorrectas: devolvemos null para que la vista
                // muestre el mensaje de error correspondiente
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
     * La contraseña se hashea antes de almacenarla. Tras la inserción,
     * recupera el ID generado automáticamente por MySQL y lo asigna
     * al objeto usuario.
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
            // Hasheamos la contraseña antes de guardarla en la base de datos
            stmt.setString(3, hashPassword(usuario.getPassword()));
            stmt.setInt(4, usuario.getRol().getIdRol());
            // last_login puede ser null si es un usuario recién creado
            stmt.setTimestamp(5, usuario.getLastLogin() != null ? Timestamp.valueOf(usuario.getLastLogin()) : null);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear el usuario: sin filas afectadas.");
            }

            // Recuperamos el ID generado por MySQL tras la inserción
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
     * Si el campo password del objeto usuario no está vacío, también
     * actualiza la contraseña (hasheándola antes de guardarla). Si está
     * vacío, la contraseña actual se mantiene sin cambios.
     *
     * Este comportamiento se implementa construyendo la query dinámicamente
     * según si hay nueva contraseña o no, usando un índice de parámetros
     * variable para asignar correctamente cada valor.
     *
     * @param usuario objeto con los datos actualizados (debe tener ID válido)
     * @return el mismo objeto usuario con los datos actualizados
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Usuario update(Usuario usuario) {

        // Construimos la query según si se quiere actualizar la contraseña o no.
        // Esto evita sobreescribir la contraseña existente cuando no se proporciona una
        // nueva.
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

            // Usamos un índice variable para asignar los parámetros correctamente
            // dependiendo de si incluimos o no la contraseña en la query
            int indice = 3;
            if (actualizarPassword) {
                stmt.setString(indice++, hashPassword(usuario.getPassword()));
            }

            stmt.setInt(indice++, usuario.getRol().getIdRol());
            stmt.setTimestamp(indice++,
                    usuario.getLastLogin() != null ? Timestamp.valueOf(usuario.getLastLogin()) : null);
            // El ID va al final para que coincida con el WHERE de la query
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
     * Hace un JOIN con Rol para devolver el usuario con su rol ya cargado,
     * evitando una segunda consulta.
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
                // Devolvemos null para que el llamante decida cómo manejar
                // la ausencia del usuario
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
     * Incluye el rol de cada usuario mediante JOIN.
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
     * Restablece la contraseña de un usuario.
     *
     * La nueva contraseña se hashea antes de almacenarla, igual que
     * en el proceso de creación de usuario.
     *
     * @param usuarioId     identificador del usuario
     * @param nuevoPassword nueva contraseña en texto plano
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public void resetPassword(Integer usuarioId, String nuevoPassword) {

        String sql = "UPDATE Usuario SET contrasena = ? WHERE id_usuario = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hasheamos la nueva contraseña antes de guardarla
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
     * Centraliza el mapeo de columnas de la base de datos a atributos
     * del modelo. Incluye el rol del usuario obtenido mediante JOIN.
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

        // last_login puede ser null si el usuario nunca ha iniciado sesión
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            usuario.setLastLogin(lastLogin.toLocalDateTime());
        }

        // Construimos el objeto Rol con los datos obtenidos del JOIN
        Rol rol = new Rol();
        rol.setIdRol(rs.getInt("id_rol"));
        rol.setNombre(rs.getString("rol_nombre"));
        rol.setDescripcion(rs.getString("rol_descripcion"));

        usuario.setRol(rol);
        return usuario;
    }

    /**
     * Genera el hash SHA-256 de una contraseña en texto plano.
     *
     * Se usa para almacenar y comparar contraseñas de forma segura,
     * evitando guardarlas en texto plano en la base de datos.
     *
     * NOTA: SHA-256 sin salt es vulnerable a ataques de diccionario y
     * tablas rainbow. Una mejora futura sería sustituirlo por BCrypt,
     * que incorpora salt automáticamente y es el estándar actual para
     * el hashing de contraseñas.
     *
     * @param password contraseña en texto plano
     * @return hash SHA-256 de la contraseña en formato Base64
     * @throws RuntimeException si el algoritmo SHA-256 no está disponible
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            // Convertimos el array de bytes a Base64 para almacenarlo como texto en BD
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Error al generar el hash de la contraseña", e);
            throw new RuntimeException("Error al generar el hash de la contraseña", e);
        }
    }
}