package controllers;

import models.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Controlador que gestiona todas las operaciones de base de datos
 * relacionadas con la entidad Cliente.
 *
 * Hereda de BaseController para reutilizar la lógica de conexión
 * y el logger compartido. Cada método abre su propia conexión y
 * la cierra automáticamente al terminar gracias a try-with-resources.
 */
public class ClienteController extends BaseController {

    /**
     * Inserta un nuevo cliente en la base de datos.
     *
     * Tras la inserción, recupera el ID generado automáticamente por MySQL
     * y lo asigna al objeto cliente, de forma que el llamante recibe
     * el cliente ya con su identificador definitivo.
     *
     * @param cliente objeto con los datos del cliente a crear
     * @return el mismo objeto cliente con el ID asignado
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Cliente create(Cliente cliente) {

        String sql = "INSERT INTO Cliente (nombre, telefono, direccion, email, empresa) VALUES (?, ?, ?, ?, ?)";

        // try-with-resources cierra automáticamente la conexión y el statement
        // al salir del bloque, incluso si ocurre una excepción.
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Asignamos cada campo del cliente a su parámetro en la query.
            // Usamos PreparedStatement para prevenir inyección SQL.
            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getDireccion());
            stmt.setString(4, cliente.getEmail());
            stmt.setString(5, cliente.getEmpresa());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Creación de cliente fallida: sin filas afectadas.");
            }

            // Recuperamos el ID generado por MySQL tras la inserción
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setIdCliente(rs.getInt(1));
                    return cliente;
                } else {
                    throw new SQLException("Error al crear el cliente: no se obtuvo ID generado.");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear cliente", e);
            throw new RuntimeException("Error de la base de datos al crear el cliente", e);
        }
    }

    /**
     * Actualiza los datos de un cliente existente en la base de datos.
     *
     * Identifica el registro a modificar por su ID. Si no se encuentra
     * ningún cliente con ese ID, lanza una excepción.
     *
     * @param cliente objeto con los datos actualizados (debe tener ID válido)
     * @return el mismo objeto cliente con los datos actualizados
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Cliente update(Cliente cliente) {

        String sql = "UPDATE Cliente SET nombre = ?, telefono = ?, direccion = ?, email = ?, empresa = ? WHERE id_cliente = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getDireccion());
            stmt.setString(4, cliente.getEmail());
            stmt.setString(5, cliente.getEmpresa());
            // El ID va al final para que coincida con el WHERE de la query
            stmt.setInt(6, cliente.getIdCliente());

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al actualizar el cliente: no hay filas afectadas.");
            }

            return cliente;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar el cliente", e);
            throw new RuntimeException("Error de la base de datos al actualizar el cliente", e);
        }
    }

    /**
     * Elimina un cliente de la base de datos por su ID.
     *
     * @param id identificador del cliente a eliminar
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public void delete(Integer id) {

        String sql = "DELETE FROM Cliente WHERE id_cliente = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int filasModificadas = stmt.executeUpdate();

            if (filasModificadas == 0) {
                throw new SQLException("Error al eliminar el cliente: no hay filas afectadas.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar cliente", e);
            throw new RuntimeException("Error de la base de datos al eliminar el cliente", e);
        }
    }

    /**
     * Busca y devuelve un cliente por su ID.
     *
     * @param id identificador del cliente a buscar
     * @return el cliente encontrado, o null si no existe
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public Cliente findById(Integer id) {

        String sql = "SELECT * FROM Cliente WHERE id_cliente = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapClienteFromResultSet(rs);
                }
                // Si no hay resultados, devolvemos null para que el llamante
                // pueda decidir cómo manejar la ausencia del cliente
                return null;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar cliente por ID", e);
            throw new RuntimeException("Error de la base de datos al buscar el cliente", e);
        }
    }

    /**
     * Devuelve la lista completa de clientes registrados en el sistema.
     *
     * @return lista de clientes (vacía si no hay ninguno)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Cliente> findAll() {

        String sql = "SELECT * FROM Cliente";
        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            // Recorremos todos los resultados y los convertimos en objetos Cliente
            while (rs.next()) {
                clientes.add(mapClienteFromResultSet(rs));
            }

            return clientes;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todos los clientes", e);
            throw new RuntimeException("Error de la base de datos al obtener los clientes", e);
        }
    }

    /**
     * Busca clientes cuyo nombre o empresa contengan el término indicado.
     *
     * Utiliza el operador LIKE con comodines (%) para hacer una búsqueda
     * parcial, de forma que "gar" encontraría "Garage Norte" o "García S.L."
     *
     * @param terminoBusqueda texto a buscar en nombre o empresa
     * @return lista de clientes que coinciden (vacía si no hay resultados)
     * @throws RuntimeException si ocurre un error en la base de datos
     */
    public List<Cliente> buscarPorNombre(String terminoBusqueda) {

        String sql = "SELECT * FROM Cliente WHERE nombre LIKE ? OR empresa LIKE ?";
        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Los % alrededor del término permiten encontrar coincidencias
            // en cualquier posición del texto (búsqueda parcial)
            String patronBusqueda = "%" + terminoBusqueda + "%";
            stmt.setString(1, patronBusqueda);
            stmt.setString(2, patronBusqueda);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapClienteFromResultSet(rs));
                }
            }

            return clientes;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar clientes por nombre", e);
            throw new RuntimeException("Error de la base de datos al buscar clientes", e);
        }
    }

    /**
     * Convierte una fila del ResultSet en un objeto Cliente.
     *
     * Centraliza el mapeo de columnas de la base de datos a atributos
     * del modelo, evitando duplicar este código en cada método de consulta.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Cliente con los datos de esa fila
     * @throws SQLException si alguna columna no existe o hay error de lectura
     */
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