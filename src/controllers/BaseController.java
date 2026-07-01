package controllers;

import config.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseController {
    protected static final Logger LOGGER = Logger.getLogger(BaseController.class.getName());

    protected Connection getConnection() throws SQLException {
        try {
            return DatabaseConnection.getConnection();
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener conexión a la base de datos", e);
            throw new SQLException("Error de conexión a la base de datos", e);
        }
    }

    protected void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar el conjunto de resultados", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar la declaración preparada", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar la conexión", e);
            }
        }
    }
}