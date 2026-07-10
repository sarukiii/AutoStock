package controllers;

import models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Controlador que gestiona todas las operaciones de base de datos
 * relacionadas con la entidad Venta y sus detalles (DetalleVenta).
 *
 * Es el controlador más complejo del sistema porque las operaciones
 * de creación y eliminación de ventas implican múltiples tablas
 * (Venta, Detalle_Venta y stock de Producto) que deben modificarse
 * de forma atómica mediante transacciones.
 *
 * Una transacción garantiza que si cualquier parte del proceso falla,
 * todos los cambios se revierten (rollback), evitando inconsistencias
 * en la base de datos (por ejemplo, venta registrada pero stock no descontado).
 *
 * Hereda de BaseController para reutilizar la lógica de conexión.
 */
public class VentaController extends BaseController {

    // Reutilizamos el ProductoController para actualizar el stock
    // desde este controlador, evitando duplicar la lógica de actualización
    private final ProductoController productoController;

    public VentaController() {
        this.productoController = new ProductoController();
    }

    /**
     * Crea una nueva venta junto con todos sus detalles en la base de datos.
     *
     * Esta operación implica tres pasos que deben ejecutarse de forma atómica:
     * 1. Insertar el registro de la venta en la tabla Venta.
     * 2. Descontar el stock de cada producto vendido.
     * 3. Insertar cada línea de detalle en la tabla Detalle_Venta.
     *
     * Si cualquier paso falla (por ejemplo, stock insuficiente), se hace
     * rollback de toda la operación para mantener la integridad de los datos.
     *
     * Usamos batch para insertar todos los detalles en una sola llamada
     * a la base de datos, lo que es más eficiente que insertar uno a uno.
     *
     * Nota: en este método no usamos try-with-resources porque necesitamos
     * control explícito de la transacción (commit/rollback) antes de cerrar
     * la conexión.
     *
     * @param venta    objeto con los datos de la venta (cliente, usuario, total)
     * @param detalles lista de líneas de detalle (producto, cantidad, precio)
     * @return la venta creada con su ID asignado
     * @throws RuntimeException si ocurre un error o hay stock insuficiente
     */
    public Venta createVenta(Venta venta, List<DetalleVenta> detalles) {

        Connection conn = null;
        PreparedStatement stmtVenta = null;
        PreparedStatement stmtDetalle = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            // Desactivamos el autocommit para controlar la transacción manualmente.
            // Así podemos confirmar (commit) o revertir (rollback) todos los cambios
            // juntos.
            conn.setAutoCommit(false);

            // --- PASO 1: Insertar la cabecera de la venta ---
            String sqlVenta = "INSERT INTO Venta (id_cliente, id_usuario, fecha, total) VALUES (?, ?, ?, ?)";
            stmtVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS);

            stmtVenta.setInt(1, venta.getCliente().getIdCliente());
            stmtVenta.setInt(2, venta.getUsuario().getIdUsuario());
            // La fecha se establece en el momento de la venta, no viene del formulario
            stmtVenta.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmtVenta.setBigDecimal(4, venta.getTotal());

            int filasModificadas = stmtVenta.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear la venta: sin filas afectadas.");
            }

            // Recuperamos el ID generado para asociar los detalles a esta venta
            rs = stmtVenta.getGeneratedKeys();
            if (rs.next()) {
                venta.setIdVenta(rs.getInt(1));
            } else {
                throw new SQLException("Error al crear la venta: no se obtuvo ID generado.");
            }

            // --- PASO 2 y 3: Descontar stock e insertar detalles ---
            String sqlDetalle = "INSERT INTO Detalle_Venta (id_venta, id_producto, cantidad, precio_unitario) " +
                    "VALUES (?, ?, ?, ?)";
            stmtDetalle = conn.prepareStatement(sqlDetalle);

            for (DetalleVenta detalle : detalles) {
                // Descontamos el stock del producto. Si no hay suficiente stock,
                // updateStock devuelve false y abortamos toda la transacción.
                if (!productoController.updateStock(detalle.getProducto().getIdProducto(), -detalle.getCantidad())) {
                    throw new SQLException("Stock insuficiente para el producto con ID: " +
                            detalle.getProducto().getIdProducto());
                }

                // Añadimos cada detalle al batch para ejecutarlos todos de una vez
                stmtDetalle.setInt(1, venta.getIdVenta());
                stmtDetalle.setInt(2, detalle.getProducto().getIdProducto());
                stmtDetalle.setInt(3, detalle.getCantidad());
                stmtDetalle.setBigDecimal(4, detalle.getPrecioUnitario());
                stmtDetalle.addBatch();
            }

            // Ejecutamos todos los detalles en una sola llamada a la BD (más eficiente)
            stmtDetalle.executeBatch();

            // Confirmamos la transacción: todos los cambios quedan guardados de forma
            // permanente
            conn.commit();
            return venta;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear la venta, revirtiendo transacción", e);
            // Si algo falla, revertimos todos los cambios realizados en esta transacción
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al revertir la transacción", ex);
            }
            throw new RuntimeException("Error de la base de datos al crear la venta", e);
        } finally {
            // Cerramos todos los recursos manualmente en el finally
            // porque necesitábamos control manual de la transacción
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar el ResultSet", e);
                }
            }
            if (stmtVenta != null) {
                try {
                    stmtVenta.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar el statement de venta", e);
                }
            }
            if (stmtDetalle != null) {
                try {
                    stmtDetalle.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar el statement de detalle", e);
                }
            }
            if (conn != null) {
                try {
                    // Restauramos el autocommit antes de cerrar la conexión
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar la conexión", e);
                }
            }
        }
    }

    /**
     * Elimina una venta y restaura el stock de los productos vendidos.
     *
     * Al igual que la creación, la eliminación es una operación transaccional
     * que afecta a varias tablas:
     * 1. Restaurar el stock de cada producto del detalle.
     * 2. Eliminar los registros de Detalle_Venta.
     * 3. Eliminar el registro de Venta.
     *
     * El orden es importante: primero restauramos el stock antes de borrar
     * los detalles, para poder saber qué cantidad devolver a cada producto.
     *
     * @param id identificador de la venta a eliminar
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public void delete(Integer id) {

        Connection conn = null;
        PreparedStatement stmtDetalles = null;
        PreparedStatement stmtVenta = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // --- PASO 1: Obtener los detalles para saber qué stock restaurar ---
            List<DetalleVenta> detalles = findDetallesByVentaId(id);

            // Devolvemos al stock la cantidad de cada producto que estaba en la venta
            for (DetalleVenta detalle : detalles) {
                productoController.updateStock(
                        detalle.getProducto().getIdProducto(),
                        detalle.getCantidad() // Cantidad positiva = sumar al stock
                );
            }

            // --- PASO 2: Eliminar los detalles de la venta ---
            // Hay que borrar los detalles antes que la venta por la restricción
            // de clave foránea (Detalle_Venta referencia a Venta)
            String sqlDetalles = "DELETE FROM Detalle_Venta WHERE id_venta = ?";
            stmtDetalles = conn.prepareStatement(sqlDetalles);
            stmtDetalles.setInt(1, id);
            stmtDetalles.executeUpdate();

            // --- PASO 3: Eliminar la venta ---
            String sqlVenta = "DELETE FROM Venta WHERE id_venta = ?";
            stmtVenta = conn.prepareStatement(sqlVenta);
            stmtVenta.setInt(1, id);
            stmtVenta.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar la venta, revirtiendo transacción", e);
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al revertir la transacción", ex);
            }
            throw new RuntimeException("Error de la base de datos al eliminar la venta", e);
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al restaurar el autocommit", ex);
            }
            closeResources(null, stmtDetalles, null);
            closeResources(conn, stmtVenta, null);
        }
    }

    /**
     * Busca y devuelve una venta por su ID, incluyendo sus detalles.
     *
     * Hace JOIN con Cliente y Usuario para obtener todos los datos
     * en una sola consulta, y luego carga los detalles por separado.
     *
     * @param id identificador de la venta a buscar
     * @return la venta encontrada con sus detalles, o null si no existe
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Venta findById(Integer id) {

        String sql = "SELECT v.*, " +
                "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                "u.nombre as usuario_nombre, u.email as usuario_email " +
                "FROM Venta v " +
                "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                "WHERE v.id_venta = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Venta venta = mapVentaFromResultSet(rs);
                    // Cargamos los detalles de la venta en una consulta separada
                    venta.setDetalles(findDetallesByVentaId(id));
                    return venta;
                }
                return null;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar venta por ID", e);
            throw new RuntimeException("Error de la base de datos al buscar la venta", e);
        }
    }

    /**
     * Devuelve todas las ventas registradas en el sistema, ordenadas por fecha
     * descendente.
     *
     * Cada venta incluye sus detalles cargados, lo que implica una consulta
     * adicional por cada venta. Para un volumen elevado de ventas, esto
     * podría optimizarse con un JOIN directo.
     *
     * @return lista de ventas con sus detalles (vacía si no hay ninguna)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Venta> findAll() {

        String sql = "SELECT v.*, " +
                "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                "u.nombre as usuario_nombre, u.email as usuario_email " +
                "FROM Venta v " +
                "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                "ORDER BY v.fecha DESC";

        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Venta venta = mapVentaFromResultSet(rs);
                venta.setDetalles(findDetallesByVentaId(venta.getIdVenta()));
                ventas.add(venta);
            }

            return ventas;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todas las ventas", e);
            throw new RuntimeException("Error de la base de datos al obtener todas las ventas", e);
        }
    }

    /**
     * Devuelve el historial de ventas de un producto concreto con evolución de
     * stock.
     *
     * Para cada venta en la que aparece el producto, calcula el stock antes y
     * después de esa venta usando una subconsulta acumulada. Esto permite mostrar
     * la evolución del stock a lo largo del tiempo en el panel de informes.
     *
     * @param productoId identificador del producto
     * @return lista de DTOs con el historial de ventas y evolución de stock
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<HistorialVentasDTO> findVentaHistoryByProductId(Integer productoId) {

        // La subconsulta calcula el total acumulado vendido hasta cada fecha,
        // lo que nos permite calcular el stock en cada momento histórico
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

        List<HistorialVentasDTO> historial = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // El productoId aparece dos veces: en la subconsulta y en el WHERE principal
            stmt.setInt(1, productoId);
            stmt.setInt(2, productoId);

            // Calculamos el stock inicial (stock actual + todo lo vendido)
            // para poder reconstruir la evolución histórica
            Integer stockInicial = getInicialStock(productoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HistorialVentasDTO dto = new HistorialVentasDTO();
                    dto.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    dto.setIdVenta(rs.getInt("id_venta"));
                    dto.setCantidad(rs.getInt("cantidad"));
                    dto.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    dto.setPrecioProducto(rs.getBigDecimal("precio"));
                    dto.setClienteNombre(rs.getString("cliente_nombre"));

                    // Calculamos el stock antes y después de esta venta
                    int acumulado = rs.getInt("acumulado");
                    dto.setNuevoValor(stockInicial - acumulado);
                    dto.setAnteriorValor(stockInicial - acumulado + dto.getCantidad());

                    historial.add(dto);
                }
            }

            return historial;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener el historial de ventas del producto", e);
            throw new RuntimeException("Error de la base de datos al obtener el historial de ventas", e);
        }
    }

    /**
     * Calcula el stock inicial de un producto (stock actual + todo lo vendido).
     *
     * Se usa para reconstruir la evolución histórica del stock en el historial
     * de ventas. La fórmula es: stock_inicial = stock_actual +
     * suma_de_todo_lo_vendido.
     *
     * COALESCE devuelve 0 si no hay ventas del producto (evita null en la suma).
     *
     * @param productoId identificador del producto
     * @return stock inicial del producto, o 0 si no existe
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    private Integer getInicialStock(Integer productoId) {

        String sql = "SELECT cantidad_disponible + " +
                "(SELECT COALESCE(SUM(cantidad), 0) " +
                "FROM Detalle_Venta dv " +
                "WHERE dv.id_producto = ?) as stock_inicial " +
                "FROM Producto " +
                "WHERE id_producto = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productoId);
            stmt.setInt(2, productoId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al calcular el stock inicial del producto", e);
            throw new RuntimeException("Error de la base de datos al calcular el stock inicial", e);
        }
    }

    /**
     * Devuelve los detalles (líneas de producto) de una venta por su ID.
     *
     * Hace JOIN con Producto para devolver cada detalle con los datos
     * del producto ya cargados.
     *
     * @param ventaId identificador de la venta
     * @return lista de detalles de la venta (vacía si no hay ninguno)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<DetalleVenta> findDetallesByVentaId(Integer ventaId) {

        String sql = "SELECT dv.*, " +
                "p.nombre as producto_nombre, p.descripcion, p.precio, " +
                "p.cantidad_disponible, p.id_proveedor " +
                "FROM Detalle_Venta dv " +
                "JOIN Producto p ON dv.id_producto = p.id_producto " +
                "WHERE dv.id_venta = ?";

        List<DetalleVenta> detalles = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ventaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    detalles.add(mapDetalleVentaFromResultSet(rs));
                }
            }

            return detalles;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener los detalles de la venta", e);
            throw new RuntimeException("Error de la base de datos al obtener los detalles de la venta", e);
        }
    }

    /**
     * Devuelve todas las ventas asociadas a un cliente concreto.
     *
     * @param clienteId identificador del cliente
     * @return lista de ventas del cliente ordenadas por fecha descendente
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Venta> findByClienteId(Integer clienteId) {

        String sql = "SELECT v.*, " +
                "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                "u.nombre as usuario_nombre, u.email as usuario_email " +
                "FROM Venta v " +
                "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                "WHERE v.id_cliente = ? " +
                "ORDER BY v.fecha DESC";

        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Venta venta = mapVentaFromResultSet(rs);
                    venta.setDetalles(findDetallesByVentaId(venta.getIdVenta()));
                    ventas.add(venta);
                }
            }

            return ventas;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener las ventas del cliente", e);
            throw new RuntimeException("Error de la base de datos al obtener las ventas del cliente", e);
        }
    }

    /**
     * Devuelve todas las ventas realizadas en un rango de fechas.
     *
     * Útil para generar informes por período (día, semana, mes, etc.).
     *
     * @param fechaInicio fecha y hora de inicio del rango (inclusive)
     * @param fechaFin    fecha y hora de fin del rango (inclusive)
     * @return lista de ventas en ese rango ordenadas por fecha descendente
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Venta> findAllBetweenDates(LocalDateTime fechaInicio, LocalDateTime fechaFin) {

        String sql = "SELECT v.*, " +
                "c.nombre as cliente_nombre, c.telefono, c.direccion, c.email, c.empresa, " +
                "u.nombre as usuario_nombre, u.email as usuario_email " +
                "FROM Venta v " +
                "JOIN Cliente c ON v.id_cliente = c.id_cliente " +
                "JOIN Usuario u ON v.id_usuario = u.id_usuario " +
                "WHERE v.fecha BETWEEN ? AND ? " +
                "ORDER BY v.fecha DESC";

        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fechaFin));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Venta venta = mapVentaFromResultSet(rs);
                    venta.setDetalles(findDetallesByVentaId(venta.getIdVenta()));
                    ventas.add(venta);
                }
            }

            return ventas;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas en el rango de fechas", e);
            throw new RuntimeException("Error de la base de datos al obtener ventas por rango de fechas", e);
        }
    }

    /**
     * Convierte una fila del ResultSet en un objeto Venta con Cliente y Usuario.
     *
     * Centraliza el mapeo de columnas a atributos del modelo. Incluye los datos
     * básicos del cliente y del usuario que realizó la venta, obtenidos mediante
     * JOIN.
     * Los detalles de la venta se cargan por separado con findDetallesByVentaId.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Venta con cliente y usuario asociados
     * @throws SQLException si alguna columna no existe o hay error de lectura
     */
    private Venta mapVentaFromResultSet(ResultSet rs) throws SQLException {
        Venta venta = new Venta();
        venta.setIdVenta(rs.getInt("id_venta"));
        venta.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        venta.setTotal(rs.getBigDecimal("total"));

        // Construimos el objeto Cliente con los datos obtenidos del JOIN
        Cliente cliente = new Cliente();
        cliente.setIdCliente(rs.getInt("id_cliente"));
        cliente.setNombre(rs.getString("cliente_nombre"));
        cliente.setTelefono(rs.getString("telefono"));
        cliente.setDireccion(rs.getString("direccion"));
        cliente.setEmail(rs.getString("email"));
        cliente.setEmpresa(rs.getString("empresa"));
        venta.setCliente(cliente);

        // Construimos el objeto Usuario con los datos obtenidos del JOIN
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setNombre(rs.getString("usuario_nombre"));
        usuario.setEmail(rs.getString("usuario_email"));
        venta.setUsuario(usuario);

        return venta;
    }

    /**
     * Convierte una fila del ResultSet en un objeto DetalleVenta con su Producto.
     *
     * Centraliza el mapeo de columnas a atributos del modelo. Incluye los datos
     * del producto obtenidos mediante JOIN para evitar una segunda consulta.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto DetalleVenta con el producto asociado
     * @throws SQLException si alguna columna no existe o hay error de lectura
     */
    private DetalleVenta mapDetalleVentaFromResultSet(ResultSet rs) throws SQLException {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setIdDetalle(rs.getInt("id_detalle"));
        detalle.setCantidad(rs.getInt("cantidad"));
        detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));

        // Construimos el objeto Producto con los datos obtenidos del JOIN
        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombre(rs.getString("producto_nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setCantidadDisponible(rs.getInt("cantidad_disponible"));
        detalle.setProducto(producto);

        return detalle;
    }
}