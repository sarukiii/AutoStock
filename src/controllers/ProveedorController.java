package controllers;

import models.Proveedor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Controlador que gestiona todas las operaciones de base de datos
 * relacionadas con la entidad Proveedor.
 *
 * Hereda de BaseController para reutilizar la lógica de conexión
 * y el logger compartido. Cada método abre su propia conexión y
 * la cierra automáticamente gracias a try-with-resources.
 */
public class ProveedorController extends BaseController {

    /**
     * Inserta un nuevo proveedor en la base de datos.
     *
     * Tras la inserción, recupera el ID generado automáticamente por MySQL
     * y lo asigna al objeto proveedor.
     *
     * @param proveedor objeto con los datos del proveedor a crear
     * @return el mismo objeto proveedor con el ID asignado
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Proveedor create(Proveedor proveedor) {

        String sql = "INSERT INTO Proveedor (nombre, contacto, telefono, direccion, email) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Asignamos cada campo del proveedor a su parámetro en la query.
            // Usamos PreparedStatement para prevenir inyección SQL.
            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getContacto());
            stmt.setString(3, proveedor.getTelefono());
            stmt.setString(4, proveedor.getDireccion());
            stmt.setString(5, proveedor.getEmail());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al crear el proveedor: sin filas afectadas.");
            }

            // Recuperamos el ID generado por MySQL tras la inserción
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    proveedor.setIdProveedor(rs.getInt(1));
                    return proveedor;
                } else {
                    throw new SQLException("Error al crear el proveedor: no se obtuvo ID generado.");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear proveedor", e);
            throw new RuntimeException("Error de la base de datos al crear el proveedor", e);
        }
    }

    /**
     * Actualiza los datos de un proveedor existente en la base de datos.
     *
     * @param proveedor objeto con los datos actualizados (debe tener ID válido)
     * @return el mismo objeto proveedor con los datos actualizados
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Proveedor update(Proveedor proveedor) {

        String sql = "UPDATE Proveedor SET nombre = ?, contacto = ?, telefono = ?, direccion = ?, email = ? WHERE id_proveedor = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getContacto());
            stmt.setString(3, proveedor.getTelefono());
            stmt.setString(4, proveedor.getDireccion());
            stmt.setString(5, proveedor.getEmail());
            // El ID va al final para que coincida con el WHERE de la query
            stmt.setInt(6, proveedor.getIdProveedor());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el proveedor: no hay filas afectadas.");
            }

            return proveedor;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar proveedor", e);
            throw new RuntimeException("Error de la base de datos al actualizar el proveedor", e);
        }
    }

    /**
     * Elimina un proveedor de la base de datos por su ID.
     *
     * @param id identificador del proveedor a eliminar
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public void delete(Integer id) {

        String sql = "DELETE FROM Proveedor WHERE id_proveedor = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al eliminar el proveedor: no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar proveedor", e);
            throw new RuntimeException("Error de la base de datos al eliminar el proveedor", e);
        }
    }

    /**
     * Busca y devuelve un proveedor por su ID.
     *
     * @param id identificador del proveedor a buscar
     * @return el proveedor encontrado, o null si no existe
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Proveedor findById(Integer id) {

        String sql = "SELECT * FROM Proveedor WHERE id_proveedor = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapProveedorFromResultSet(rs);
                }
                // Devolvemos null para que el llamante decida cómo manejar
                // la ausencia del proveedor
                return null;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar proveedor por ID", e);
            throw new RuntimeException("Error de la base de datos al buscar el proveedor", e);
        }
    }

    /**
     * Devuelve la lista completa de proveedores registrados en el sistema.
     *
     * @return lista de proveedores (vacía si no hay ninguno)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Proveedor> findAll() {

        String sql = "SELECT * FROM Proveedor";
        List<Proveedor> proveedores = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                proveedores.add(mapProveedorFromResultSet(rs));
            }

            return proveedores;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todos los proveedores", e);
            throw new RuntimeException("Error de la base de datos al obtener los proveedores", e);
        }
    }

    /**
     * Busca proveedores cuyo nombre o contacto contengan el término indicado.
     *
     * Utiliza el operador LIKE con comodines (%) para hacer una búsqueda
     * parcial, de forma que "dis" encontraría "Distribuciones Norte" o
     * "Distelec S.L."
     *
     * @param terminoBusqueda texto a buscar en nombre o contacto
     * @return lista de proveedores que coinciden (vacía si no hay resultados)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Proveedor> searchByName(String terminoBusqueda) {

        String sql = "SELECT * FROM Proveedor WHERE nombre LIKE ? OR contacto LIKE ?";
        List<Proveedor> proveedores = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Los % alrededor del término permiten encontrar coincidencias
            // en cualquier posición del texto (búsqueda parcial)
            String patronBusqueda = "%" + terminoBusqueda + "%";
            stmt.setString(1, patronBusqueda);
            stmt.setString(2, patronBusqueda);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    proveedores.add(mapProveedorFromResultSet(rs));
                }
            }

            return proveedores;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar proveedores por nombre", e);
            throw new RuntimeException("Error de la base de datos al buscar proveedores", e);
        }
    }

    /**
     * Convierte una fila del ResultSet en un objeto Proveedor.
     *
     * Centraliza el mapeo de columnas de la base de datos a atributos
     * del modelo, evitando duplicar este código en cada método de consulta.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Proveedor con los datos de esa fila
     * @throws SQLException si alguna columna no existe o hay error de lectura
     */
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