package controllers;

import models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class VentaController extends BaseController {

    private final ProductoController productoController;

    public VentaController() {
        this.productoController = new ProductoController();
    }

    public Venta createVenta(Venta venta, List<DetalleVenta> detalles) {
        Connection conn = null;
        PreparedStatement stmtVenta = null;
        PreparedStatement stmtDetalle = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Inicio transacción

            // Insertar venta
            String sqlVenta = "INSERT INTO Venta (id_cliente, id_usuario, fecha, total) VALUES (?, ?, ?, ?)";
            stmtVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS);

            stmtVenta.setInt(1, venta.getCliente().getIdCliente());
            stmtVenta.setInt(2, venta.getUsuario().getIdUsuario());
            stmtVenta.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmtVenta.setBigDecimal(4, venta.getTotal());

            int filasModificadas = stmtVenta.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear la venta, no se afectaron las filas.");
            }

            rs = stmtVenta.getGeneratedKeys();
            if (rs.next()) {
                venta.setIdVenta(rs.getInt(1));
            } else {
                throw new SQLException("Error al crear la venta, no se obtuvo ninguna Id");
            }

            // Insertar datos DetalleVenta
            String sqlDetalle = "INSERT INTO Detalle_Venta (id_venta, id_producto, cantidad, precio_unitario) " +
                    "VALUES (?, ?, ?, ?)";
            stmtDetalle = conn.prepareStatement(sqlDetalle);

            for (DetalleVenta detalle : detalles) {
                // Actualizar stock
                if (!productoController.updateStock(detalle.getProducto().getIdProducto(), -detalle.getCantidad())) {
                    throw new SQLException("Stock insuficiente para el producto: " + detalle.getProducto().getIdProducto());
                }

                stmtDetalle.setInt(1, venta.getIdVenta());
                stmtDetalle.setInt(2, detalle.getProducto().getIdProducto());
                stmtDetalle.setInt(3, detalle.getCantidad());
                stmtDetalle.setBigDecimal(4, detalle.getPrecioUnitario());
                stmtDetalle.addBatch();
            }

            stmtDetalle.executeBatch();
            conn.commit(); //  Confirmar transacción
            return venta;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la venta", e);
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al revertir la transacción", ex);
            }
            throw new RuntimeException("Error de la base de datos al crear la venta", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar el conjunto de resultados", e);
                }
            }
            if (stmtVenta != null) {
                try {
                    stmtVenta.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar la declaración", e);
                }
            }
            if (stmtDetalle != null) {
                try {
                    stmtDetalle.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar la declaración", e);
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar la conexión", e);
                }
            }
        }
    }

    public void delete(Integer id) {
        Connection conn = null;
        PreparedStatement stmtDetalles = null;
        PreparedStatement stmtVenta = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1º obtener los detalles de la venta para restaurar el stock
            List<DetalleVenta> detalles = findDetallesByVentaId(id);

            // Restaurar stock para cada producto
            for (DetalleVenta detalle : detalles) {
                productoController.updateStock(
                        detalle.getProducto().getIdProducto(),
                        detalle.getCantidad()
                );
            }

            // Eliminar detalles de venta
            String sqlDetalles = "DELETE FROM Detalle_Venta WHERE id_venta = ?";
            stmtDetalles = conn.prepareStatement(sqlDetalles);
            stmtDetalles.setInt(1, id);
            stmtDetalles.executeUpdate();

            // Borrar venta
            String sqlVenta = "DELETE FROM Venta WHERE id_venta = ?";
            stmtVenta = conn.prepareStatement(sqlVenta);
            stmtVenta.setInt(1, id);
            stmtVenta.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al revertir la transacción", ex);
            }
            LOGGER.log(Level.SEVERE, "Error al eliminar la venta", e);
            throw new RuntimeException("Error de la base de datos al eliminar la venta", e);
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al restablecer la confirmación automática", ex);
            }
            closeResources(null, stmtDetalles, null);
            closeResources(conn, stmtVenta, null);
        }
    }

    public Venta findById(Integer id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT v.*, " +
                    "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                    "u.nombre as usuario_nombre, u.email as usuario_email " +
                    "FROM Venta v " +
                    "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                    "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                    "WHERE v.id_venta = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                Venta venta = mapVentaFromResultSet(rs);
                venta.setDetalles(findDetallesByVentaId(id));
                return venta;
            }

            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar venta por ID", e);
            throw new RuntimeException("Error de la base de datos al encontrar la venta", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Venta> findAll() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Venta> ventas = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT v.*, " +
                    "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                    "u.nombre as usuario_nombre, u.email as usuario_email " +
                    "FROM Venta v " +
                    "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                    "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                    "ORDER BY v.fecha DESC";

            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Venta venta = mapVentaFromResultSet(rs);
                venta.setDetalles(findDetallesByVentaId(venta.getIdVenta()));
                ventas.add(venta);
            }

            return ventas;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar todas las ventas", e);
            throw new RuntimeException("Error de la base de datos al encontrar todas las ventasc", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<HistorialVentasDTO> findVentaHistoryByProductId(Integer productoId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<HistorialVentasDTO> historial = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT v.id_venta, v.fecha, v.total, " +
                    "dv.cantidad, dv.precio_unitario, " +
                    "c.id_cliente, c.nombre as cliente_nombre, " +
                    "p.id_producto, p.nombre as producto_nombre, p.precio, " +
                    "(SELECT SUM(cantidad) " +
                    "FROM Detalle_Venta dv2 " +
                    "JOIN Venta v2 ON dv2.id_venta = v2.id_venta " +
                    "WHERE dv2.id_producto = ? " +
                    "AND v2.fecha <= v.fecha) as acumulado " +
                    "FROM Venta v " +
                    "JOIN Detalle_Venta dv ON v.id_venta = dv.id_venta " +
                    "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                    "JOIN Producto p ON dv.id_producto = p.id_producto " +
                    "WHERE dv.id_producto = ? " +
                    "ORDER BY v.fecha DESC";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productoId);
            stmt.setInt(2, productoId);

            rs = stmt.executeQuery();

            // Obtener stock inicial
            Integer inicialStock = getInicialStock(productoId);

            while (rs.next()) {
                HistorialVentasDTO dto = new HistorialVentasDTO();
                dto.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                dto.setIdVenta(rs.getInt("id_venta"));
                dto.setCantidad(rs.getInt("cantidad"));
                dto.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                dto.setPrecioProducto(rs.getBigDecimal("precio"));
                dto.setClienteNombre(rs.getString("cliente_nombre"));

                // Obtener stock inicial
                int acumulado = rs.getInt("acumulado");
                dto.setNuevoValor(inicialStock - acumulado);
                dto.setAnteriorValor(inicialStock - acumulado + dto.getCantidad());

                historial.add(dto);
            }

            return historial;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener el historial de ventas", e);
            throw new RuntimeException("Error de la base de datos al obtener el historial de ventas", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private Integer getInicialStock(Integer productoId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT cantidad_disponible + " +
                    "(SELECT COALESCE(SUM(cantidad), 0) " +
                    "FROM Detalle_Venta dv " +
                    "WHERE dv.id_producto = ?) as stock_inicial " +
                    "FROM Producto " +
                    "WHERE id_producto = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productoId);
            stmt.setInt(2, productoId);

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro al obtener el stock inicial", e);
            throw new RuntimeException("Error de la base de datos al obtener el stock inicial", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<DetalleVenta> findDetallesByVentaId(Integer ventaId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<DetalleVenta> detalles = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT dv.*, " +
                    "p.nombre as producto_nombre, p.descripcion, p.precio, " +
                    "p.cantidad_disponible, p.id_proveedor " +
                    "FROM Detalle_Venta dv " +
                    "JOIN Producto p ON dv.id_producto = p.id_producto " +
                    "WHERE dv.id_venta = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, ventaId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                detalles.add(mapDetalleVentaFromResultSet(rs));
            }

            return detalles;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener stock inicial", e);
            throw new RuntimeException("Error de base de datos al obtener stock inicial", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<Venta> findByClienteId(Integer clienteId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Venta> ventas = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT v.*, " +
                    "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                    "u.nombre as usuario_nombre, u.email as usuario_email " +
                    "FROM Venta v " +
                    "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                    "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                    "WHERE v.id_cliente = ? " +
                    "ORDER BY v.fecha DESC";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, clienteId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Venta venta = mapVentaFromResultSet(rs);
                venta.setDetalles(findDetallesByVentaId(venta.getIdVenta()));
                ventas.add(venta);
            }

            return ventas;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas por cliente", e);
            throw new RuntimeException("Error de la base de datos al obtener las ventas por cliente", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private Venta mapVentaFromResultSet(ResultSet rs) throws SQLException {
        Venta venta = new Venta();
        venta.setIdVenta(rs.getInt("id_venta"));
        venta.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        venta.setTotal(rs.getBigDecimal("total"));

        Cliente cliente = new Cliente();
        cliente.setIdCliente(rs.getInt("id_cliente"));
        cliente.setNombre(rs.getString("cliente_nombre"));
        cliente.setTelefono(rs.getString("telefono"));
        cliente.setDireccion(rs.getString("direccion"));
        cliente.setEmail(rs.getString("email"));
        cliente.setEmpresa(rs.getString("empresa"));
        venta.setCliente(cliente);

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombre(rs.getString("usuario_nombre"));
        usuario.setEmail(rs.getString("usuario_email"));
        venta.setUsuario(usuario);

        return venta;
    }

    private DetalleVenta mapDetalleVentaFromResultSet(ResultSet rs) throws SQLException {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setIdDetalle(rs.getInt("id_detalle"));
        detalle.setCantidad(rs.getInt("cantidad"));
        detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));

        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombre(rs.getString("producto_nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setCantidadDisponible(rs.getInt("cantidad_disponible"));
        detalle.setProducto(producto);

        return detalle;
    }

    public List<Venta> findAllBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Venta> ventas = new ArrayList<>();

        try {
            conn = getConnection();
            String sql = "SELECT v.*, " +
                    "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                    "u.nombre as usuario_nombre, u.email as usuario_email " +
                    "FROM Venta v " +
                    "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                    "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                    "WHERE v.fecha BETWEEN ? AND ? " +
                    "ORDER BY v.fecha DESC";

            stmt = conn.prepareStatement(sql);
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            rs = stmt.executeQuery();

            while (rs.next()) {
                Venta venta = mapVentaFromResultSet(rs);
                venta.setDetalles(findDetallesByVentaId(venta.getIdVenta()));
                ventas.add(venta);
            }

            return ventas;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al encontrar ventas en el rango de fechas", e);
            throw new RuntimeException("Error de la base de datos al encontrar ventas en el rango de fechas", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
}