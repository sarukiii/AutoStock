package controllers;

import models.Producto;
import models.Proveedor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Controlador que gestiona todas las operaciones de base de datos
 * relacionadas con la entidad Producto.
 *
 * Además del CRUD básico, incluye operaciones específicas de negocio
 * como la actualización de stock y la búsqueda por proveedor.
 * Hereda de BaseController para reutilizar la lógica de conexión.
 */
public class ProductoController extends BaseController {

    /**
     * Inserta un nuevo producto en la base de datos.
     *
     * Tras la inserción, recupera el ID generado automáticamente por MySQL
     * y lo asigna al objeto producto.
     *
     * @param producto objeto con los datos del producto a crear
     * @return el mismo objeto producto con el ID asignado
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Producto create(Producto producto) {

        String sql = "INSERT INTO Producto (nombre, descripcion, precio, cantidad_disponible, id_proveedor) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Asignamos cada campo del producto a su parámetro en la query.
            // Usamos PreparedStatement para prevenir inyección SQL.
            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.setInt(4, producto.getCantidadDisponible());
            stmt.setInt(5, producto.getProveedor().getIdProveedor());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear el producto: sin filas afectadas.");
            }

            // Recuperamos el ID generado por MySQL tras la inserción
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    producto.setIdProducto(rs.getInt(1));
                    return producto;
                } else {
                    throw new SQLException("Error al crear el producto: no se obtuvo ID generado.");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear producto", e);
            throw new RuntimeException("Error de la base de datos al crear el producto", e);
        }
    }

    /**
     * Actualiza los datos de un producto existente en la base de datos.
     *
     * @param producto objeto con los datos actualizados (debe tener ID válido)
     * @return el mismo objeto producto con los datos actualizados
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Producto update(Producto producto) {

        String sql = "UPDATE Producto SET nombre = ?, descripcion = ?, precio = ?, " +
                "cantidad_disponible = ?, id_proveedor = ? WHERE id_producto = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.setInt(4, producto.getCantidadDisponible());
            stmt.setInt(5, producto.getProveedor().getIdProveedor());
            // El ID va al final para que coincida con el WHERE de la query
            stmt.setInt(6, producto.getIdProducto());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el producto: no hay filas afectadas.");
            }

            return producto;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar el producto", e);
            throw new RuntimeException("Error de la base de datos al actualizar el producto", e);
        }
    }

    /**
     * Elimina un producto de la base de datos por su ID.
     *
     * @param id identificador del producto a eliminar
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public void delete(Integer id) {

        String sql = "DELETE FROM Producto WHERE id_producto = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al eliminar el producto: no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar el producto", e);
            throw new RuntimeException("Error de la base de datos al eliminar el producto", e);
        }
    }

    /**
     * Busca y devuelve un producto por su ID.
     *
     * Hace un JOIN con Proveedor para devolver el producto con todos
     * los datos de su proveedor ya cargados, evitando una segunda consulta.
     *
     * @param id identificador del producto a buscar
     * @return el producto encontrado, o null si no existe
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Producto findById(Integer id) {

        // JOIN con Proveedor para obtener todos los datos en una sola consulta
        String sql = "SELECT p.*, pr.nombre as proveedor_nombre, pr.contacto, pr.telefono, " +
                "pr.direccion, pr.email FROM Producto p " +
                "JOIN Proveedor pr ON p.id_proveedor = pr.id_proveedor " +
                "WHERE p.id_producto = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapProductoFromResultSet(rs);
                }
                // Devolvemos null para que el llamante decida cómo manejar
                // la ausencia del producto
                return null;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar el producto por ID", e);
            throw new RuntimeException("Error de la base de datos al buscar el producto", e);
        }
    }

    /**
     * Devuelve la lista completa de productos registrados en el sistema.
     *
     * Incluye los datos del proveedor de cada producto mediante JOIN.
     *
     * @return lista de productos (vacía si no hay ninguno)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Producto> findAll() {

        String sql = "SELECT p.*, pr.nombre as proveedor_nombre, pr.contacto, pr.telefono, " +
                "pr.direccion, pr.email FROM Producto p " +
                "JOIN Proveedor pr ON p.id_proveedor = pr.id_proveedor";

        List<Producto> productos = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                productos.add(mapProductoFromResultSet(rs));
            }

            return productos;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todos los productos", e);
            throw new RuntimeException("Error de la base de datos al obtener los productos", e);
        }
    }

    /**
     * Devuelve todos los productos suministrados por un proveedor concreto.
     *
     * Útil para filtrar el catálogo por proveedor en la interfaz.
     *
     * @param proveedorId identificador del proveedor
     * @return lista de productos de ese proveedor (vacía si no hay ninguno)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Producto> findByProveedor(Integer proveedorId) {

        String sql = "SELECT p.*, pr.nombre as proveedor_nombre, pr.contacto, pr.telefono, " +
                "pr.direccion, pr.email FROM Producto p " +
                "JOIN Proveedor pr ON p.id_proveedor = pr.id_proveedor " +
                "WHERE p.id_proveedor = ?";

        List<Producto> productos = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, proveedorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapProductoFromResultSet(rs));
                }
            }

            return productos;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar productos por proveedor", e);
            throw new RuntimeException("Error de la base de datos al buscar productos por proveedor", e);
        }
    }

    /**
     * Actualiza el stock de un producto sumando (o restando) la cantidad indicada.
     *
     * La query incluye una condición que impide que el stock quede en negativo:
     * solo actualiza si (stock_actual + cantidad) >= 0. Esto protege la
     * integridad del inventario sin necesidad de hacer una consulta previa.
     *
     * Para restar stock (en una venta), se pasa cantidad negativa.
     * Para sumar stock (en una recepción), se pasa cantidad positiva.
     *
     * @param productoId identificador del producto
     * @param cantidad   cantidad a sumar (positiva) o restar (negativa)
     * @return true si el stock se actualizó correctamente, false si no hay
     *         suficiente stock disponible
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public boolean updateStock(Integer productoId, Integer cantidad) {

        // La condición (cantidad_disponible + ?) >= 0 evita stock negativo.
        // Si no se cumple, executeUpdate devuelve 0 filas y retornamos false.
        String sql = "UPDATE Producto SET cantidad_disponible = cantidad_disponible + ? " +
                "WHERE id_producto = ? AND (cantidad_disponible + ?) >= 0";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cantidad);
            stmt.setInt(2, productoId);
            // Repetimos cantidad en el tercer parámetro para la condición del WHERE
            stmt.setInt(3, cantidad);

            int filasModificadas = stmt.executeUpdate();
            // Si se modificó al menos una fila, el stock se actualizó correctamente
            return filasModificadas > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar el stock del producto", e);
            throw new RuntimeException("Error de la base de datos al actualizar el stock", e);
        }
    }

    /**
     * Convierte una fila del ResultSet en un objeto Producto con su Proveedor.
     *
     * Centraliza el mapeo de columnas de la base de datos a atributos
     * del modelo. Al incluir el proveedor, evita tener que hacer una
     * segunda consulta para obtener sus datos.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Producto con todos sus datos y el proveedor asociado
     * @throws SQLException si alguna columna no existe o hay error de lectura
     */
    private Producto mapProductoFromResultSet(ResultSet rs) throws SQLException {

        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setCantidadDisponible(rs.getInt("cantidad_disponible"));

        // Construimos el objeto Proveedor con los datos obtenidos del JOIN
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