package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.IOException;

/**
 * Clase utilitaria para gestionar la conexión a la base de datos MySQL.
 * 
 * Utiliza el patrón de configuración externa: las credenciales se cargan
 * desde el archivo db.properties en lugar de estar escritas directamente
 * en el código fuente. Esto evita exponer datos sensibles en el repositorio
 * y facilita cambiar la configuración sin tocar el código.
 */
public class DatabaseConnection {

    // Logger para registrar eventos de conexión y errores.
    // Se usa el nombre de la clase para identificar fácilmente el origen del log.
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    // Credenciales de conexión cargadas desde db.properties al arrancar la
    // aplicación.
    // Son constantes estáticas porque la configuración no cambia durante la
    // ejecución.
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    /*
     * Bloque estático de inicialización.
     * Se ejecuta una sola vez cuando la clase se carga en memoria, antes de
     * cualquier llamada a sus métodos. Carga el archivo db.properties desde
     * el classpath y extrae las propiedades de conexión.
     * 
     * Si el archivo no existe o no se puede leer, se lanza una excepción
     * en tiempo de arranque para detectar el error lo antes posible.
     */
    static {
        Properties props = new Properties();

        // try-with-resources garantiza que el InputStream se cierra
        // automáticamente al terminar el bloque, aunque ocurra una excepción.
        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("config/db.properties")) {

            if (input == null) {
                throw new RuntimeException(
                        "No se encontró el archivo db.properties. " +
                                "Copia db.properties.example como db.properties y configura tus credenciales.");
            }

            props.load(input);

        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo db.properties", e);
        }

        // Extraemos cada propiedad por su clave definida en el archivo .properties
        URL = props.getProperty("db.url");
        USER = props.getProperty("db.user");
        PASSWORD = props.getProperty("db.password");
    }

    /**
     * Abre y devuelve una nueva conexión a la base de datos.
     * 
     * Cada llamada a este método crea una conexión nueva. Es responsabilidad
     * del código que llama a este método cerrar la conexión cuando ya no la
     * necesite, preferiblemente usando try-with-resources.
     * 
     * @return Connection objeto de conexión activa a la base de datos
     * @throws DatabaseException si el driver no se encuentra o la conexión falla
     */
    public static Connection getConnection() throws DatabaseException {
        try {
            // Cargamos explícitamente el driver JDBC de MySQL.
            // En versiones modernas del conector esto no es estrictamente necesario
            // (se carga automáticamente), pero lo mantenemos por compatibilidad y claridad.
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            LOGGER.info("Conexión a la base de datos establecida correctamente");
            return connection;

        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver JDBC de MySQL no encontrado", e);
            throw new DatabaseException("Driver de base de datos no encontrado", e);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al conectar con la base de datos", e);
            throw new DatabaseException("Error al conectar con la base de datos", e);
        }
    }

    /**
     * Cierra la conexión estática si está abierta.
     * 
     * Nota: este método opera sobre el campo estático 'connection', que actualmente
     * no se usa en getConnection() (cada llamada crea su propia conexión local).
     * Se mantiene por si en el futuro se implementa un patrón de conexión
     * compartida.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Conexión a la base de datos cerrada correctamente");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cerrar la conexión a la base de datos", e);
        }
    }

    // Campo estático reservado para una posible conexión compartida futura.
    // Actualmente no se utiliza en getConnection().
    private static Connection connection = null;
}