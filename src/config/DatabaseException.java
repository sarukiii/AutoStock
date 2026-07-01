package config;

/**
 * Excepción personalizada para errores relacionados con la base de datos.
 *
 * Extender Exception (en lugar de usar SQLException directamente) permite
 * desacoplar las capas de la aplicación: los controladores lanzan
 * DatabaseException sin que las vistas necesiten conocer los detalles
 * de JDBC. Así, si en el futuro se cambia el sistema de persistencia,
 * solo hay que modificar la capa de datos.
 *
 * Es una excepción "checked" (comprobada), lo que obliga al código que
 * la recibe a decidir explícitamente cómo manejarla.
 */
public class DatabaseException extends Exception {

    /**
     * Crea una nueva DatabaseException con un mensaje descriptivo y la causa
     * original.
     *
     * @param message descripción del error ocurrido
     * @param cause   excepción original que provocó este error (por ejemplo, una
     *                SQLException)
     */
    public DatabaseException(String message, Throwable cause) {
        // Llamamos al constructor de Exception para propagar mensaje y causa,
        // lo que permite ver el stack trace completo al depurar.
        super(message, cause);
    }
}