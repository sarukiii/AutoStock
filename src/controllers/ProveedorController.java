package controllers;

import models.Proveedor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ProveedorController extends BaseController {

    public Proveedor create(Proveedor proveedor) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "INSERT INTO Proveedor (nombre, contacto, telefono, direccion, email) VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getContacto());
            stmt.setString(3, proveedor.getTelefono());
            stmt.setString(4, proveedor.getDireccion());
            stmt.setString(5, proveedor.getEmail());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear el proveedor, no hay filas afectadas.");
            }

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                proveedor.setIdProveedor(rs.getInt(1));
                return proveedor;
            } else {
                throw new SQLException("Error al crear el proveedor, no se obtuvo ningun ID.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear proveedor", e);
            throw new RuntimeException("Error de la base de datos al crear proveedor", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Proveedor update(Proveedor proveedor) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "UPDATE Proveedor SET nombre = ?, contacto = ?, telefono = ?, direccion = ?, email = ? WHERE id_proveedor = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getContacto());
            stmt.setString(3, proveedor.getTelefono());
            stmt.setString(4, proveedor.getDireccion());
            stmt.setString(5, proveedor.getEmail());
            stmt.setInt(6, proveedor.getIdProveedor());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el proveedor, no hay filas afectadas.");
            }

            return proveedor;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar proveedor", e);
            throw new RuntimeException("Error de base de datos al actualizar el proveedor", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public void delete(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "DELETE FROM Proveedor WHERE id_proveedor = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al eliminar el proveedor, no se afectarán las filas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar proveedor", e);
            throw new RuntimeException("Error de la base de datos al eliminar proveedor", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public Proveedor findById(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT * FROM Proveedor WHERE id_proveedor = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapProveedorFromResultSet(rs);
            }

            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar proveedor por ID", e);
            throw new RuntimeException("Error de base de datos al encontrar proveedor", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Proveedor> findAll() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Proveedor> proveedores = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT * FROM Proveedor";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                proveedores.add(mapProveedorFromResultSet(rs));
            }

            return proveedores;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar todos los proveedores", e);
            throw new RuntimeException("Error de la base de datos al encontrar todos los proveedores", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Proveedor> searchByName(String terminoBusqueda) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Proveedor> proveedores = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT * FROM Proveedor WHERE nombre LIKE ? OR contacto LIKE ?";
            stmt = conn.prepareStatement(sql);
            String patronBusqueda = "%" + terminoBusqueda + "%";
            stmt.setString(1, patronBusqueda);
            stmt.setString(2, patronBusqueda);

            rs = stmt.executeQuery();

            while (rs.next()) {
                proveedores.add(mapProveedorFromResultSet(rs));
            }

            return proveedores;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar proveedores por nombre", e);
            throw new RuntimeException("Error de la base de datos al buscar proveedores", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private Proveedor mapProveedorFromResultSet(ResultSet rs) throws SQLException {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(rs.getInt("id_proveedor"));
        proveedor.setNombre(rs.getString("nombre"));
        proveedor.setContacto(rs.getString("contacto"));
        proveedor.setTelefono(rs.getString("telefono"));
        proveedor.setDireccion(rs.getString("direccion"));
        proveedor.setEmail(rs.getString("email"));
        return proveedor;
    }
}
