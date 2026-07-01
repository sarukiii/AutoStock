package controllers;

import models.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ClienteController extends BaseController {

    public Cliente create(Cliente cliente) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "INSERT INTO Cliente (nombre, telefono, direccion, email, empresa) VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getDireccion());
            stmt.setString(4, cliente.getEmail());
            stmt.setString(5, cliente.getEmpresa());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Creación de cliente fallida, sin filas afectadas.");
            }

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                cliente.setIdCliente(rs.getInt(1));
                return cliente;
            } else {
                throw new SQLException("Error al crear el cliente, no se obtuvo ID.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear cliente", e);
            throw new RuntimeException("Error de la base de datos al crear el cliente", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Cliente update(Cliente cliente) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "UPDATE Cliente SET nombre = ?, telefono = ?, direccion = ?, email = ?, empresa = ? WHERE id_cliente = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getDireccion());
            stmt.setString(4, cliente.getEmail());
            stmt.setString(5, cliente.getEmpresa());
            stmt.setInt(6, cliente.getIdCliente());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el cliente, no hay filas afectadas.");
            }

            return cliente;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar el cliente", e);
            throw new RuntimeException("Error de la base de datos al actualizar el cliente", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public void delete(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "DELETE FROM Cliente WHERE id_cliente = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al eliminar el cliente, no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar cliente", e);
            throw new RuntimeException("Error de base de datos al eliminar el cliente", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public Cliente findById(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT * FROM Cliente WHERE id_cliente = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapClienteFromResultSet(rs);
            }

            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar cliente por ID", e);
            throw new RuntimeException("Error de la base de datos al encontrar el cliente", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Cliente> findAll() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Cliente> clientes = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT * FROM Cliente";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                clientes.add(mapClienteFromResultSet(rs));
            }

            return clientes;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar todos los clientes", e);
            throw new RuntimeException("Error de la base de datos al encontrar todos los clientes", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Cliente> buscarPorNombre(String terminoBusqueda) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Cliente> clientes = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT * FROM Cliente WHERE nombre LIKE ? OR empresa LIKE ?";
            stmt = conn.prepareStatement(sql);
            String patronBusqueda = "%" + terminoBusqueda + "%";
            stmt.setString(1, patronBusqueda);
            stmt.setString(2, patronBusqueda);

            rs = stmt.executeQuery();

            while (rs.next()) {
                clientes.add(mapClienteFromResultSet(rs));
            }

            return clientes;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar clientes por nombre", e);
            throw new RuntimeException("Error de la base de datos al buscar clientes", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private Cliente mapClienteFromResultSet(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(rs.getInt("id_cliente"));
        cliente.setNombre(rs.getString("nombre"));
        cliente.setTelefono(rs.getString("telefono"));
        cliente.setDireccion(rs.getString("direccion"));
        cliente.setEmail(rs.getString("email"));
        cliente.setEmpresa(rs.getString("empresa"));
        return cliente;
    }
}