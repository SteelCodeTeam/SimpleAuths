package team.steelcode.simpleauths.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import team.steelcode.simpleauths.database.services.DatabaseInitService;

import javax.sql.DataSource;

/**
 * Configuración de la base de datos con HikariCP - JDBC Puro
 */
public class DatabaseConfig {

    private static HikariDataSource dataSource;

    public static String userTableName = "players";
    public static String ipHistoryTableName = "ip_history";

    // Enum para dialectos SQL
    public enum SQLDialect {
        MYSQL, MARIADB, POSTGRES, SQLITE, H2
    }

    public static SQLDialect sqlDialect = SQLDialect.MYSQL;

    // Datos de conexión - estos deberían venir de un archivo de configuración
    public static String jdbcUrl = "";
    public static String username = "";
    public static String password = "";

    /**
     * Inicializa la conexión a la base de datos con HikariCP
     */
    public static void initialize(String url, String user, String pass, String userTable, String ipTable, String dialectName) {
        jdbcUrl = url;
        username = user;
        password = pass;
        userTableName = userTable;
        ipHistoryTableName = ipTable;

        // Determinar dialecto SQL basado en el nombre
        sqlDialect = getSQLDialectFromName(dialectName);

        try {
            // Configurar HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(getDriverClassName(sqlDialect));

            // Configuraciones del pool de conexiones
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000); // 5 minutos
            config.setConnectionTimeout(30000); // 30 segundos
            config.setLeakDetectionThreshold(60000); // 1 minuto
            config.setMaxLifetime(1800000); // 30 minutos

            // Configuraciones específicas por base de datos
            configureDatabase(config, sqlDialect);

            // Crear el DataSource
            dataSource = new HikariDataSource(config);

            // Inicializar base de datos
            new DatabaseInitService().initializeDatabase();

        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar la conexión a la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Inicializa con configuración por defecto
     */
    public static void initialize() {
        initialize(jdbcUrl, username, password, userTableName, ipHistoryTableName, sqlDialect.name());
    }

    /**
     * Obtiene el DataSource de HikariCP
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("La base de datos no ha sido inicializada. Llame a initialize() primero.");
        }
        return dataSource;
    }

    /**
     * Obtiene el nombre de la tabla de usuarios
     */
    public static String getUserTableName() {
        return userTableName;
    }

    /**
     * Obtiene el nombre de la tabla de historial IP
     */
    public static String getIpHistoryTableName() {
        return ipHistoryTableName;
    }

    /**
     * Obtiene el dialecto SQL configurado
     */
    public static SQLDialect getSQLDialect() {
        return sqlDialect;
    }

    /**
     * Cierra el pool de conexiones
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }

    /**
     * Determina el dialecto SQL basado en el nombre
     */
    private static SQLDialect getSQLDialectFromName(String dialectName) {
        if (dialectName == null || dialectName.isEmpty()) {
            return SQLDialect.MYSQL; // Valor predeterminado
        }

        try {
            return SQLDialect.valueOf(dialectName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si no se reconoce, usar MySQL como predeterminado
            return SQLDialect.MYSQL;
        }
    }

    /**
     * Obtiene el nombre de clase del driver JDBC basado en el dialecto
     */
    public static String getDriverClassName(SQLDialect dialect) {
        return switch (dialect) {
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case POSTGRES -> "org.postgresql.Driver";
            case SQLITE -> "org.sqlite.JDBC";
            case H2 -> "org.h2.Driver";
            // Agregar otros dialectos según sea necesario
            default -> "com.mysql.cj.jdbc.Driver"; // Por defecto MySQL
        };
    }

    /**
     * Configura propiedades específicas de la base de datos
     */
    private static void configureDatabase(HikariConfig config, SQLDialect dialect) {
        switch (dialect) {
            case MYSQL, MARIADB -> {
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
            }
            case POSTGRES -> {
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("preparedStatementCacheQueries", "256");
                config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
                config.addDataSourceProperty("defaultRowFetchSize", "1000");
            }
            case SQLITE -> {
                config.setMaximumPoolSize(1); // SQLite solo permite una conexión de escritura
                config.setMinimumIdle(1);
            }
        }
    }

    /**
     * Obtiene estadísticas del pool de conexiones
     */
    public static String getPoolStats() {
        if (dataSource == null) {
            return "Pool no inicializado";
        }

        return String.format(
                "Pool Stats - Total: %d, Activas: %d, Inactivas: %d, Esperando: %d",
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
}