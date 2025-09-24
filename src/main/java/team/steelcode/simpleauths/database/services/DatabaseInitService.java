package team.steelcode.simpleauths.database.services;

import team.steelcode.simpleauths.database.DatabaseConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para inicializar la base de datos y crear tablas si no existen
 * Migrado a JDBC puro con HikariCP
 */
public class DatabaseInitService {

    private static final Logger LOGGER = Logger.getLogger("SimpleAuths");
    private final String userTableName;
    private final String ipHistoryTableName;

    public DatabaseInitService() {
        this.userTableName = DatabaseConfig.getUserTableName();
        this.ipHistoryTableName = DatabaseConfig.getIpHistoryTableName();
    }

    /**
     * Inicializa la base de datos creando las tablas si no existen
     */
    public void initializeDatabase() {
        LOGGER.info("Inicializando base de datos de SimpleAuths...");

        Connection connection = null;
        try {
            // Obtener conexión del pool de HikariCP
            connection = DatabaseConfig.getDataSource().getConnection();
            connection.setAutoCommit(false); // Iniciar transacción

            // Verificar si las tablas existen
            boolean userTableExists = tableExists(connection, userTableName);
            boolean ipHistoryTableExists = tableExists(connection, ipHistoryTableName);

            // Crear tablas si no existen
            if (!userTableExists) {
                LOGGER.info("Creando tabla de usuarios: " + userTableName);
                createUserTable(connection);
            } else {
                LOGGER.info("Tabla de usuarios ya existe: " + userTableName);
            }

            if (!ipHistoryTableExists) {
                LOGGER.info("Creando tabla de historial IP: " + ipHistoryTableName);
                createIPHistoryTable(connection);
            } else {
                LOGGER.info("Tabla de historial IP ya existe: " + ipHistoryTableName);
            }

            // Confirmar transacción
            connection.commit();
            LOGGER.info("Inicialización de base de datos completada exitosamente.");

        } catch (Exception e) {
            // Rollback en caso de error
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Error durante rollback", rollbackEx);
                }
            }
            LOGGER.log(Level.SEVERE, "Error al inicializar la base de datos: " + e.getMessage(), e);
            throw new RuntimeException("Error al inicializar la base de datos", e);
        } finally {
            // Cerrar conexión (la devuelve al pool)
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restaurar auto-commit
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar conexión", e);
                }
            }
        }
    }

    /**
     * Verifica si una tabla existe en la base de datos usando DatabaseMetaData
     */
    private boolean tableExists(Connection connection, String tableName) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();

            // Buscar tabla en diferentes esquemas según la base de datos
            String[] tableTypes = {"TABLE"};

            try (ResultSet tables = metaData.getTables(null, null, tableName, tableTypes)) {
                if (tables.next()) {
                    return true;
                }
            }

            // Intentar con nombre en mayúsculas (algunos DBMS son case-sensitive)
            try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), tableTypes)) {
                if (tables.next()) {
                    return true;
                }
            }

            // Intentar con nombre en minúsculas
            try (ResultSet tables = metaData.getTables(null, null, tableName.toLowerCase(), tableTypes)) {
                return tables.next();
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al verificar existencia de tabla " + tableName + ", intentando método alternativo", e);
            return tableExistsAlternative(connection, tableName);
        }
    }

    /**
     * Método alternativo para verificar existencia de tabla usando consultas específicas
     */
    private boolean tableExistsAlternative(Connection connection, String tableName) {
        String query = getTableExistsQuery();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error en método alternativo para verificar tabla " + tableName, e);
        }

        return false;
    }

    /**
     * Obtiene la consulta SQL para verificar existencia de tabla según el dialecto
     */
    private String getTableExistsQuery() {
        return switch (DatabaseConfig.getSQLDialect()) {
            case MYSQL, MARIADB ->
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = DATABASE()";
            case POSTGRES ->
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = 'public'";
            case SQLITE ->
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name = ?";
            case H2 ->
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = UPPER(?)";
            default ->
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
        };
    }

    /**
     * Crea la tabla de usuarios con sintaxis adaptada al dialecto SQL
     */
    private void createUserTable(Connection connection) throws SQLException {
        String createTableSQL = buildUserTableSQL();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);

            // Crear índices para búsqueda rápida
            createUserTableIndexes(connection);
        }
    }

    /**
     * Construye el SQL para crear la tabla de usuarios según el dialecto
     */
    private String buildUserTableSQL() {
        String autoIncrement = switch (DatabaseConfig.getSQLDialect()) {
            case MYSQL, MARIADB -> "AUTO_INCREMENT";
            case POSTGRES -> "SERIAL";
            case SQLITE -> "AUTOINCREMENT";
            case H2 -> "AUTO_INCREMENT";
            default -> "AUTO_INCREMENT";
        };

        String timestampDefault = switch (DatabaseConfig.getSQLDialect()) {
            case MYSQL, MARIADB -> "CURRENT_TIMESTAMP";
            case POSTGRES -> "CURRENT_TIMESTAMP";
            case SQLITE -> "CURRENT_TIMESTAMP";
            case H2 -> "CURRENT_TIMESTAMP";
            default -> "CURRENT_TIMESTAMP";
        };

        return switch (DatabaseConfig.getSQLDialect()) {
            case POSTGRES -> String.format(
                    "CREATE TABLE %s (" +
                            "player_id BIGSERIAL PRIMARY KEY, " +
                            "uuid VARCHAR(36) NOT NULL UNIQUE, " +
                            "username VARCHAR(32) NOT NULL, " +
                            "password VARCHAR(128) NOT NULL, " +
                            "email VARCHAR(64), " +
                            "play_time BIGINT DEFAULT 0, " +
                            "max_ips_allowed INTEGER DEFAULT 3, " +
                            "requires_ip_verification BOOLEAN DEFAULT FALSE, " +
                            "created_at TIMESTAMP DEFAULT %s, " +
                            "last_login TIMESTAMP DEFAULT %s" +
                            ")", userTableName, timestampDefault, timestampDefault
            );
            case SQLITE -> String.format(
                    "CREATE TABLE %s (" +
                            "player_id INTEGER PRIMARY KEY %s, " +
                            "uuid TEXT NOT NULL UNIQUE, " +
                            "username TEXT NOT NULL, " +
                            "password TEXT NOT NULL, " +
                            "email TEXT, " +
                            "play_time INTEGER DEFAULT 0, " +
                            "max_ips_allowed INTEGER DEFAULT 3, " +
                            "requires_ip_verification INTEGER DEFAULT 0, " +
                            "created_at TEXT DEFAULT %s, " +
                            "last_login TEXT DEFAULT %s" +
                            ")", userTableName, autoIncrement, timestampDefault, timestampDefault
            );
            default -> String.format(
                    "CREATE TABLE %s (" +
                            "player_id BIGINT %s PRIMARY KEY, " +
                            "uuid VARCHAR(36) NOT NULL UNIQUE, " +
                            "username VARCHAR(32) NOT NULL, " +
                            "password VARCHAR(128) NOT NULL, " +
                            "email VARCHAR(64), " +
                            "play_time BIGINT DEFAULT 0, " +
                            "max_ips_allowed INT DEFAULT 3, " +
                            "requires_ip_verification BOOLEAN DEFAULT FALSE, " +
                            "created_at TIMESTAMP DEFAULT %s, " +
                            "last_login TIMESTAMP DEFAULT %s" +
                            ")", userTableName, autoIncrement, timestampDefault, timestampDefault
            );
        };
    }

    /**
     * Crea los índices para la tabla de usuarios
     */
    private void createUserTableIndexes(Connection connection) throws SQLException {
        String[] indexes = {
                String.format("CREATE INDEX idx_%s_uuid ON %s (uuid)", userTableName, userTableName),
                String.format("CREATE INDEX idx_%s_username ON %s (username)", userTableName, userTableName)
        };

        try (Statement stmt = connection.createStatement()) {
            for (String indexSQL : indexes) {
                try {
                    stmt.execute(indexSQL);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error creando índice: " + indexSQL, e);
                }
            }
        }
    }

    /**
     * Crea la tabla de historial de IPs
     */
    private void createIPHistoryTable(Connection connection) throws SQLException {
        String createTableSQL = buildIPHistoryTableSQL();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);

            // Crear índices para búsqueda rápida
            createIPHistoryTableIndexes(connection);
        }
    }

    /**
     * Construye el SQL para crear la tabla de historial IP según el dialecto
     */
    private String buildIPHistoryTableSQL() {
        String autoIncrement = switch (DatabaseConfig.getSQLDialect()) {
            case MYSQL, MARIADB -> "AUTO_INCREMENT";
            case POSTGRES -> "SERIAL";
            case SQLITE -> "AUTOINCREMENT";
            case H2 -> "AUTO_INCREMENT";
            default -> "AUTO_INCREMENT";
        };

        String timestampDefault = switch (DatabaseConfig.getSQLDialect()) {
            case MYSQL, MARIADB -> "CURRENT_TIMESTAMP";
            case POSTGRES -> "CURRENT_TIMESTAMP";
            case SQLITE -> "CURRENT_TIMESTAMP";
            case H2 -> "CURRENT_TIMESTAMP";
            default -> "CURRENT_TIMESTAMP";
        };

        return switch (DatabaseConfig.getSQLDialect()) {
            case POSTGRES -> String.format(
                    "CREATE TABLE %s (" +
                            "id BIGSERIAL PRIMARY KEY, " +
                            "user_id BIGINT NOT NULL, " +
                            "ip_address VARCHAR(45) NOT NULL, " +
                            "first_seen TIMESTAMP DEFAULT %s, " +
                            "last_seen TIMESTAMP DEFAULT %s, " +
                            "login_count INTEGER DEFAULT 1, " +
                            "is_trusted BOOLEAN DEFAULT FALSE, " +
                            "is_blocked BOOLEAN DEFAULT FALSE, " +
                            "FOREIGN KEY (user_id) REFERENCES %s(player_id) ON DELETE CASCADE" +
                            ")", ipHistoryTableName, timestampDefault, timestampDefault, userTableName
            );
            case SQLITE -> String.format(
                    "CREATE TABLE %s (" +
                            "id INTEGER PRIMARY KEY %s, " +
                            "user_id INTEGER NOT NULL, " +
                            "ip_address TEXT NOT NULL, " +
                            "first_seen TEXT DEFAULT %s, " +
                            "last_seen TEXT DEFAULT %s, " +
                            "login_count INTEGER DEFAULT 1, " +
                            "is_trusted INTEGER DEFAULT 0, " +
                            "is_blocked INTEGER DEFAULT 0, " +
                            "FOREIGN KEY (user_id) REFERENCES %s(player_id) ON DELETE CASCADE" +
                            ")", ipHistoryTableName, autoIncrement, timestampDefault, timestampDefault, userTableName
            );
            default -> String.format(
                    "CREATE TABLE %s (" +
                            "id BIGINT %s PRIMARY KEY, " +
                            "user_id BIGINT NOT NULL, " +
                            "ip_address VARCHAR(45) NOT NULL, " +
                            "first_seen TIMESTAMP DEFAULT %s, " +
                            "last_seen TIMESTAMP DEFAULT %s, " +
                            "login_count INT DEFAULT 1, " +
                            "is_trusted BOOLEAN DEFAULT FALSE, " +
                            "is_blocked BOOLEAN DEFAULT FALSE, " +
                            "FOREIGN KEY (user_id) REFERENCES %s(player_id) ON DELETE CASCADE" +
                            ")", ipHistoryTableName, autoIncrement, timestampDefault, timestampDefault, userTableName
            );
        };
    }

    /**
     * Crea los índices para la tabla de historial IP
     */
    private void createIPHistoryTableIndexes(Connection connection) throws SQLException {
        String[] indexes = {
                String.format("CREATE INDEX idx_%s_user_id ON %s (user_id)", ipHistoryTableName, ipHistoryTableName),
                String.format("CREATE INDEX idx_%s_ip_address ON %s (ip_address)", ipHistoryTableName, ipHistoryTableName),
                String.format("CREATE UNIQUE INDEX idx_%s_user_ip ON %s (user_id, ip_address)", ipHistoryTableName, ipHistoryTableName)
        };

        try (Statement stmt = connection.createStatement()) {
            for (String indexSQL : indexes) {
                try {
                    stmt.execute(indexSQL);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error creando índice: " + indexSQL, e);
                }
            }
        }
    }
}