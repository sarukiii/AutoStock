package controllers;

import models.Producto;
import models.Proveedor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ProductoController extends BaseController {

    public Producto create(Producto producto) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "INSERT INTO Producto (nombre, descripcion, precio, cantidad_disponible, id_proveedor) " +
                    "VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.setInt(4, producto.getCantidadDisponible());
            stmt.setInt(5, producto.getProveedor().getIdProveedor());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear el producto, no hay filas afectadas.");
            }

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                producto.setIdProducto(rs.getInt(1));
                return producto;
            } else {
                throw new SQLException("Error al crear el producto, no se obtuvo ninguna ID.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear producto", e);
            throw new RuntimeException("Error de la base de datos al crear el producto", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public Producto update(Producto producto) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "UPDATE Producto SET nombre = ?, descripcion = ?, precio = ?, " +
                    "cantidad_disponible = ?, id_proveedor = ? WHERE id_producto = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.setInt(4, producto.getCantidadDisponible());
            stmt.setInt(5, producto.getProveedor().getIdProveedor());
            stmt.setInt(6, producto.getIdProducto());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el producto, no hay filas afectadas.");
            }

            return producto;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar el producto", e);
            throw new RuntimeException("Error de base de datos al actualizar el producto", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public void delete(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "DELETE FROM Producto WHERE id_producto = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Error al eliminar el producto, no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar el producto", e);
            throw new RuntimeException("Error de base de datos al eliminar el producto", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public Producto findById(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT p.*, pr.nombre as proveedor_nombre, pr.contacto, pr.telefono, " +
                    "pr.direccion, pr.email FROM Producto p " +
                    "JOIN Proveedor pr ON p.id_proveedor = pr.id_proveedor " +
                    "WHERE p.id_producto = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapProductoFromResultSet(rs);
            }

            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar el producto por ID", e);
            throw new RuntimeException("Error de la base de datos al encontrar el producto", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Producto> findAll() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Producto> productos = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT p.*, pr.nombre as proveedor_nombre, pr.contacto, pr.telefono, " +
                    "pr.direccion, pr.email FROM Producto p " +
                    "JOIN Proveedor pr ON p.id_proveedor = pr.id_proveedor";

            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                productos.add(mapProductoFromResultSet(rs));
            }

            return productos;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar todos los productos", e);
            throw new RuntimeException("Error de la base de datos al encontrar todos los productos", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Producto> findByProveedor(Integer proveedorId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Producto> productos = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT p.*, pr.nombre as proveedor_nombre, pr.contacto, pr.telefono, " +
                    "pr.direccion, pr.email FROM Producto p " +
                    "JOIN Proveedor pr ON p.id_proveedor = pr.id_proveedor " +
                    "WHERE p.id_proveedor = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, proveedorId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                productos.add(mapProductoFromResultSet(rs));
            }

            return productos;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar productos por proveedor", e);
            throw new RuntimeException("Error de base de datos al buscar productos por proveedor", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean updateStock(Integer productoId, Integer cantidad) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            String sql = "UPDATE Producto SET cantidad_disponible = cantidad_disponible + ? " +
                    "WHERE id_producto = ? AND (cantidad_disponible + ?) >= 0";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cantidad);
            stmt.setInt(2, productoId);
            stmt.setInt(3, cantidad);

            int filasModificadas = stmt.executeUpdate();
            return filasModificadas > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar stock de productos", e);
            throw new RuntimeException("Error de la base de datos al actualizar stock de productos", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private Producto mapProductoFromResultSet(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setCantidadDisponible(rs.getInt("cantidad_disponible"));

        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(rs.getInt("id_proveedor"));
        proveedor.setNombre(rs.getString("proveedor_nombre"));
        proveedor.setContacto(rs.getString("contacto"));
        proveedor.setTelefono(rs.getString("telefono"));
        proveedor.setDireccion(rs.getString("direccion"));
        proveedor.setEmail(rs.getString("email"));

        producto.setProveedor(proveedor);
        return producto;
    }
}
