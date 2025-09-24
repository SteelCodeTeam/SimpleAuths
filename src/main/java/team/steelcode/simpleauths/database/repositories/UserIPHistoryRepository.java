package team.steelcode.simpleauths.database.repositories;

import team.steelcode.simpleauths.database.DatabaseConfig;
import team.steelcode.simpleauths.database.entities.UserIPHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserIPHistoryRepository {

    private static final Logger LOGGER = Logger.getLogger("SimpleAuths");
    private Connection connection;
    private boolean manageConnection; // Flag para saber si debemos cerrar la conexión

    public UserIPHistoryRepository() {
        this.connection = null;
        this.manageConnection = true; // Obtenemos conexiones del pool
    }

    // Constructor con conexión específica para transacciones
    public UserIPHistoryRepository(Connection connection) {
        this.connection = connection;
        this.manageConnection = false; // No cerramos la conexión externa
    }

    /**
     * Guarda o actualiza un registro de historial IP
     */
    public void save(UserIPHistory ipHistory) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();

        if (ipHistory.getId() == null) {
            insert(ipHistory, tableName);
        } else {
            update(ipHistory, tableName);
        }
    }

    /**
     * Inserta un nuevo registro de historial IP
     */
    private void insert(UserIPHistory ipHistory, String tableName) throws SQLException {
        String sql = String.format(
                "INSERT INTO %s (user_id, ip_address, first_seen, last_seen, login_count, is_trusted, is_blocked) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableName
        );

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setInsertParameters(stmt, ipHistory);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Error al insertar historial IP, no se insertó ningún registro");
            }

            // Obtener el ID generado
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ipHistory.setId(generatedKeys.getLong(1));
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Actualiza un registro existente de historial IP
     */
    private void update(UserIPHistory ipHistory, String tableName) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET user_id = ?, ip_address = ?, first_seen = ?, last_seen = ?, " +
                        "login_count = ?, is_trusted = ?, is_blocked = ? WHERE id = ?",
                tableName
        );

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setUpdateParameters(stmt, ipHistory);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Error al actualizar historial IP, registro no encontrado con ID: " + ipHistory.getId());
            }
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Busca un registro por usuario e IP
     */
    public Optional<UserIPHistory> findByUserAndIP(Long userId, String ipAddress) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format(
                "SELECT * FROM %s WHERE user_id = ? AND ip_address = ?",
                tableName
        );

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, ipAddress);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToIPHistory(rs));
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }

        return Optional.empty();
    }

    /**
     * Busca usuarios activos por IP excluyendo ciertos UUIDs
     */
    public List<UserIPHistory> findActiveUsersByIP(String ipAddress, List<String> excludeUuids) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String userTableName = DatabaseConfig.getUserTableName();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT h.* FROM ").append(tableName).append(" h ")
                .append("JOIN ").append(userTableName).append(" u ON h.user_id = u.player_id ")
                .append("WHERE h.ip_address = ?");

        if (excludeUuids != null && !excludeUuids.isEmpty()) {
            sql.append(" AND u.uuid NOT IN (");
            for (int i = 0; i < excludeUuids.size(); i++) {
                sql.append("?");
                if (i < excludeUuids.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
        }

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, ipAddress);

            if (excludeUuids != null && !excludeUuids.isEmpty()) {
                for (String uuid : excludeUuids) {
                    stmt.setString(paramIndex++, uuid);
                }
            }

            return executeQueryAndMapToList(stmt);
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Busca registros por ID de usuario ordenados por última conexión descendente
     */
    public List<UserIPHistory> findByUserIdOrderByLastSeenDesc(Long userId) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format(
                "SELECT * FROM %s WHERE user_id = ? ORDER BY last_seen DESC",
                tableName
        );

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return executeQueryAndMapToList(stmt);
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Cuenta las IPs recientes de un usuario desde una fecha específica
     */
    public long countRecentIPsByUser(Long userId, LocalDateTime since) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE user_id = ? AND last_seen > ?",
                tableName
        );

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(since));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }

        return 0;
    }

    /**
     * Elimina registros por ID de usuario
     */
    public int deleteByUserId(Long userId) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format("DELETE FROM %s WHERE user_id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate();
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Encuentra todas las IPs bloqueadas
     */
    public List<UserIPHistory> findBlockedIPs() throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format("SELECT * FROM %s WHERE is_blocked = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, true);
            return executeQueryAndMapToList(stmt);
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Marca una IP como bloqueada o desbloqueada
     */
    public void updateBlockedStatus(Long id, boolean blocked) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format("UPDATE %s SET is_blocked = ? WHERE id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, blocked);
            stmt.setLong(2, id);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró registro con ID: " + id);
            }
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Obtiene estadísticas de IPs por usuario
     */
    public long getTotalIPCount(Long userId) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE user_id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }

        return 0;
    }

    // ================ MÉTODOS PRIVADOS AUXILIARES ================

    /**
     * Obtiene una conexión, ya sea del pool o la proporcionada externamente
     */
    private Connection getConnection() throws SQLException {
        if (connection != null) {
            return connection;
        }
        return DatabaseConfig.getDataSource().getConnection();
    }

    /**
     * Cierra la conexión solo si la estamos gestionando nosotros
     */
    private void closeConnectionIfManaged(Connection conn) {
        if (manageConnection && conn != null && conn != connection) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar conexión", e);
            }
        }
    }

    /**
     * Ejecuta una consulta y mapea los resultados a una lista
     */
    private List<UserIPHistory> executeQueryAndMapToList(PreparedStatement stmt) throws SQLException {
        List<UserIPHistory> results = new ArrayList<>();

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSetToIPHistory(rs));
            }
        }

        return results;
    }

    /**
     * Establece los parámetros para INSERT
     */
    private void setInsertParameters(PreparedStatement stmt, UserIPHistory ipHistory) throws SQLException {
        stmt.setLong(1, ipHistory.getUserId());
        stmt.setString(2, ipHistory.getIpAddress());
        stmt.setTimestamp(3, ipHistory.getFirstSeen() != null ? Timestamp.valueOf(ipHistory.getFirstSeen()) : null);
        stmt.setTimestamp(4, ipHistory.getLastSeen() != null ? Timestamp.valueOf(ipHistory.getLastSeen()) : null);
        stmt.setInt(5, ipHistory.getLoginCount());
        stmt.setBoolean(6, ipHistory.getTrusted());
        stmt.setBoolean(7, ipHistory.getBlocked());
    }

    /**
     * Establece los parámetros para UPDATE
     */
    private void setUpdateParameters(PreparedStatement stmt, UserIPHistory ipHistory) throws SQLException {
        setInsertParameters(stmt, ipHistory);
        stmt.setLong(8, ipHistory.getId()); // WHERE id = ?
    }

    /**
     * Mapea un ResultSet a un objeto UserIPHistory
     */
    private UserIPHistory mapResultSetToIPHistory(ResultSet rs) throws SQLException {
        UserIPHistory ipHistory = new UserIPHistory();
        ipHistory.setId(rs.getLong("id"));
        ipHistory.setUserId(rs.getLong("user_id"));
        ipHistory.setIpAddress(rs.getString("ip_address"));

        // Manejo de timestamps
        Timestamp firstSeen = rs.getTimestamp("first_seen");
        if (firstSeen != null) {
            ipHistory.setFirstSeen(firstSeen.toLocalDateTime());
        }

        Timestamp lastSeen = rs.getTimestamp("last_seen");
        if (lastSeen != null) {
            ipHistory.setLastSeen(lastSeen.toLocalDateTime());
        }

        ipHistory.setLoginCount(rs.getInt("login_count"));
        ipHistory.setTrusted(rs.getBoolean("is_trusted"));
        ipHistory.setBlocked(rs.getBoolean("is_blocked"));

        return ipHistory;
    }

    /**
     * Actualiza la conexión utilizada (útil para transacciones)
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
        this.manageConnection = false;
    }

    /**
     * Resetea para usar el pool de conexiones
     */
    public void useConnectionPool() {
        this.connection = null;
        this.manageConnection = true;
    }
}