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

public class UsuarioController extends BaseController {

    public Usuario login(String email, String password) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT u.*, r.nombre as rol_nombre, r.descripcion as rol_descripcion " +
                    "FROM Usuario u " +
                    "JOIN Rol r ON u.id_rol = r.id_rol " +
                    "WHERE u.email = ? AND u.contrasena = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, hashPassword(password));

            rs = stmt.executeQuery();

            if (rs.next()) {
                // Actualizar último login
                String updateLoginSql = "UPDATE Usuario SET last_login = ? WHERE id_usuario = ?";
                PreparedStatement updateLoginStmt = conn.prepareStatement(updateLoginSql);
                updateLoginStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                updateLoginStmt.setInt(2, rs.getInt("id_usuario"));
                updateLoginStmt.executeUpdate();
                updateLoginStmt.close();

                return mapUsuarioFromResultSet(rs);
            }

            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al iniciar sesión", e);
            throw new RuntimeException("Error de base de datos durante el inicio de sesión", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Usuario create(Usuario usuario) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "INSERT INTO Usuario (nombre, email, contrasena, id_rol, last_login) VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, hashPassword(usuario.getPassword()));
            stmt.setInt(4, usuario.getRol().getIdRol());
            stmt.setTimestamp(5, usuario.getLastLogin() != null ?
                    Timestamp.valueOf(usuario.getLastLogin()) : null);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear el usuario, no hay filas afectadas.");
            }

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                usuario.setIdUsuario(rs.getInt(1));
                return usuario;
            } else {
                throw new SQLException("Error al crear el usuario, no se obtuvo ningun Id.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear usuario", e);
            throw new RuntimeException("Error de base de datos al crear usuario", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Usuario update(Usuario usuario) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "UPDATE Usuario SET nombre = ?, email = ?, id_rol = ?, last_login = ? WHERE id_usuario = ?";

            // Se puede actualizar también el password
            if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                sql = "UPDATE Usuario SET nombre = ?, email = ?, contrasena = ?, id_rol = ?, last_login = ? WHERE id_usuario = ?";
            }

            stmt = conn.prepareStatement(sql);

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());

            int paramIndex = 3;
            if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                stmt.setString(paramIndex++, hashPassword(usuario.getPassword()));
            }

            stmt.setInt(paramIndex++, usuario.getRol().getIdRol());
            stmt.setTimestamp(paramIndex++, usuario.getLastLogin() != null ?
                    Timestamp.valueOf(usuario.getLastLogin()) : null);
            stmt.setInt(paramIndex, usuario.getIdUsuario());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el usuario, no hay filas afectadas.");
            }

            return usuario;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar usuario", e);
            throw new RuntimeException("Error de la base de datos al actualizar el usuarioDatabase error while updating user", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

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

    // Otros métodos permanecen sin cambios
    public void delete(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "DELETE FROM Usuario WHERE id_usuario = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al eliminar la usuario, no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar usuario", e);
            throw new RuntimeException("Error de la base de datos al eliminar usuario", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public Usuario findById(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT u.*, r.nombre as rol_nombre, r.descripcion as rol_descripcion " +
                    "FROM Usuario u " +
                    "JOIN Rol r ON u.id_rol = r.id_rol " +
                    "WHERE u.id_usuario = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapUsuarioFromResultSet(rs);
            }

            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar usuario por ID", e);
            throw new RuntimeException("Error de base de datos al encontrar al usuario", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Usuario> findAll() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Usuario> usuarios = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT u.*, r.nombre as rol_nombre, r.descripcion as rol_descripcion " +
                    "FROM Usuario u " +
                    "JOIN Rol r ON u.id_rol = r.id_rol";

            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                usuarios.add(mapUsuarioFromResultSet(rs));
            }

            return usuarios;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar todos los usuarios", e);
            throw new RuntimeException("Error de la base de datos al encontrar todos los usuario", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Error de hash de contraseña", e);
            throw new RuntimeException("Error de hash de contraseña", e);
        }
    }

    public void resetPassword(Integer usuarioId, String nuevoPassword) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "UPDATE Usuario SET contrasena = ? WHERE id_usuario = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, hashPassword(nuevoPassword));
            stmt.setInt(2, usuarioId);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al restablecer la contraseña, no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al restablecer la contraseña", e);
            throw new RuntimeException("Error de base de datos al restablecer la contraseña", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }
}