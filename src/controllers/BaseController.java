package controllers;

import config.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase base abstracta para todos los controladores de la aplicación.
 *
 * Centraliza la lógica común a todos los controladores: obtener una conexión
 * a la base de datos y cerrar correctamente los recursos JDBC al terminar.
 * Al heredar de esta clase, los controladores evitan duplicar este código
 * en cada operación de base de datos.
 *
 * Es abstracta porque no tiene sentido instanciarla directamente —
 * solo sirve como base para los controladores concretos.
 */
public abstract class BaseController {

    // Logger compartido por todos los controladores que hereden de esta clase.
    // Al usar el nombre de BaseController, los logs de conexión quedan
    // agrupados y diferenciados de los logs de negocio de cada controlador.
    protected static final Logger LOGGER = Logger.getLogger(BaseController.class.getName());

    /**
     * Obtiene una conexión activa a la base de datos.
     *
     * Actúa como puente entre DatabaseConnection (que lanza DatabaseException)
     * y los controladores (que trabajan con SQLException), manteniendo
     * la compatibilidad con el estándar JDBC.
     *
     * @return Connection objeto de conexión listo para usar
     * @throws SQLException si no se puede establecer la conexión
     */
    protected Connection getConnection() throws SQLException {
        try {
            return DatabaseConnection.getConnection();
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener conexión a la base de datos", e);
            // Convertimos DatabaseException a SQLException para mantener
            // la firma estándar de JDBC en los controladores.
            throw new SQLException("Error de conexión a la base de datos", e);
        }
    }

    /**
     * Cierra de forma segura los recursos JDBC utilizados en una operación.
     *
     * Se cierran en orden inverso al de apertura: primero el ResultSet,
     * luego el PreparedStatement y finalmente la Connection. Esto es
     * importante porque cerrar la conexión antes que los demás recursos
     * puede provocar errores en algunos drivers.
     *
     * Cada cierre se hace en su propio try-catch para garantizar que un
     * error al cerrar un recurso no impida cerrar los siguientes.
     *
     * Nota: en los controladores, cuando sea posible, se recomienda usar
     * try-with-resources en lugar de llamar a este método manualmente,
     * ya que Java cierra los recursos automáticamente al salir del bloque.
     *
     * @param conn conexión a cerrar (puede ser null)
     * @param stmt sentencia preparada a cerrar (puede ser null)
     * @param rs   conjunto de resultados a cerrar (puede ser null)
     */
    protected void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {

        // Cerramos el ResultSet primero, ya que depende del PreparedStatement
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar el conjunto de resultados (ResultSet)", e);
            }
        }

        // Cerramos el PreparedStatement antes que la conexión
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar la sentencia preparada (PreparedStatement)", e);
            }
        }

        // Cerramos la conexión al final para liberar el recurso de red
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar la conexión a la base de datos", e);
            }
        }
    }
}