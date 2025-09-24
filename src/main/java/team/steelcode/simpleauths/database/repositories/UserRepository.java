package team.steelcode.simpleauths.database.repositories;

import team.steelcode.simpleauths.database.DatabaseConfig;
import team.steelcode.simpleauths.database.entities.User;

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

public class UserRepository {

    private static final Logger LOGGER = Logger.getLogger("SimpleAuths");
    private final String tableName;
    private Connection connection;
    private boolean manageConnection; // Flag para saber si debemos cerrar la conexión

    public UserRepository() {
        this.tableName = DatabaseConfig.getUserTableName();
        this.connection = null;
        this.manageConnection = true; // Obtenemos conexiones del pool
    }

    // Constructor con conexión específica para transacciones
    public UserRepository(Connection connection) {
        this.tableName = DatabaseConfig.getUserTableName();
        this.connection = connection;
        this.manageConnection = false;
    }

    /**
     * Guarda o actualiza un usuario
     */
    public User save(User user) throws SQLException {
        if (user.getId() == null) {
            return insert(user);
        } else {
            update(user);
            return user;
        }
    }

    /**
     * Inserta un nuevo usuario
     */
    private User insert(User user) throws SQLException {
        String sql = String.format(
                "INSERT INTO %s (uuid, username, password, email, play_time, max_ips_allowed, requires_ip_verification, created_at, last_login) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tableName
        );

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setInsertParameters(stmt, user);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Error al insertar usuario, no se insertó ningún registro");
            }

            // Obtener el ID generado
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Error al insertar usuario, no se generó ID");
                }
            }

            return user;
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Actualiza un usuario existente
     */
    private void update(User user) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET uuid = ?, username = ?, password = ?, email = ?, play_time = ?, " +
                        "max_ips_allowed = ?, requires_ip_verification = ?, last_login = ? WHERE player_id = ?",
                tableName
        );

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setUpdateParameters(stmt, user);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Error al actualizar usuario, registro no encontrado con ID: " + user.getId());
            }
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Busca un usuario por UUID
     */
    public Optional<User> findByPlayername(String playerName) throws SQLException {
        String sql = String.format("SELECT * FROM %s WHERE username = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            System.out.println("SQL: " + stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }

        return Optional.empty();
    }

    /**
     * Busca un usuario por ID
     */
    public Optional<User> findById(Long id) throws SQLException {
        String sql = String.format("SELECT * FROM %s WHERE player_id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }

        return Optional.empty();
    }

    /**
     * Busca un usuario por nombre de usuario
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = String.format("SELECT * FROM %s WHERE username = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }

        return Optional.empty();
    }

    /**
     * Busca usuarios por email (puede haber múltiples si no es único)
     */
    public List<User> findByEmail(String email) throws SQLException {
        String sql = String.format("SELECT * FROM %s WHERE email = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            return executeQueryAndMapToList(stmt);
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Elimina un usuario por UUID
     */
    public boolean deleteByUuid(String uuid) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE uuid = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Elimina un usuario por ID
     */
    public boolean deleteById(Long id) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE player_id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Verifica si existe un usuario con el nombre de usuario dado
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE username = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } finally {
            closeConnectionIfManaged(conn);
        }

        return false;
    }

    /**
     * Actualiza el tiempo de juego de un usuario
     */
    public void updatePlayTime(Long userId, Long playTime) throws SQLException {
        String sql = String.format("UPDATE %s SET play_time = ? WHERE player_id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, playTime);
            stmt.setLong(2, userId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró usuario con ID: " + userId);
            }
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Actualiza la última conexión de un usuario
     */
    public void updateLastLogin(Long userId, LocalDateTime lastLogin) throws SQLException {
        String sql = String.format("UPDATE %s SET last_login = ? WHERE player_id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(lastLogin));
            stmt.setLong(2, userId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró usuario con ID: " + userId);
            }
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Actualiza la contraseña de un usuario
     */
    public void updatePassword(Long userId, String hashedPassword) throws SQLException {
        String sql = String.format("UPDATE %s SET password = ? WHERE player_id = ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setLong(2, userId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se encontró usuario con ID: " + userId);
            }
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Obtiene todos los usuarios (con paginación opcional)
     */
    public List<User> findAll(int limit, int offset) throws SQLException {
        String sql = String.format("SELECT * FROM %s ORDER BY created_at DESC LIMIT ? OFFSET ?", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            return executeQueryAndMapToList(stmt);
        } finally {
            closeConnectionIfManaged(conn);
        }
    }

    /**
     * Cuenta el total de usuarios
     */
    public long countAll() throws SQLException {
        String sql = String.format("SELECT COUNT(*) FROM %s", tableName);

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
    private List<User> executeQueryAndMapToList(PreparedStatement stmt) throws SQLException {
        List<User> results = new ArrayList<>();

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSetToUser(rs));
            }
        }

        return results;
    }

    /**
     * Establece los parámetros para INSERT
     */
    private void setInsertParameters(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getUuid());
        stmt.setString(2, user.getUsername());
        stmt.setString(3, user.getPassword());
        stmt.setString(4, user.getEmail());
        stmt.setLong(5, user.getPlayTime() != null ? user.getPlayTime() : 0L);
        stmt.setInt(6, user.getMaxIpsAllowed() != null ? user.getMaxIpsAllowed() : 3);
        stmt.setBoolean(7, user.getRequiresIPVerification() != null ? user.getRequiresIPVerification() : false);

        // Timestamps para created_at y last_login
        LocalDateTime now = LocalDateTime.now();
        stmt.setTimestamp(8, Timestamp.valueOf(now)); // created_at
        stmt.setTimestamp(9, Timestamp.valueOf(now)); // last_login
    }

    /**
     * Establece los parámetros para UPDATE
     */
    private void setUpdateParameters(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getUuid());
        stmt.setString(2, user.getUsername());
        stmt.setString(3, user.getPassword());
        stmt.setString(4, user.getEmail());
        stmt.setLong(5, user.getPlayTime() != null ? user.getPlayTime() : 0L);
        stmt.setInt(6, user.getMaxIpsAllowed() != null ? user.getMaxIpsAllowed() : 3);
        stmt.setBoolean(7, user.getRequiresIPVerification() != null ? user.getRequiresIPVerification() : false);
        stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now())); // last_login actualizado
        stmt.setLong(9, user.getId()); // WHERE player_id = ?
    }

    /**
     * Mapea un ResultSet a un objeto User
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("player_id"));
        user.setUuid(rs.getString("uuid"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setPlayTime(rs.getLong("play_time"));
        user.setMaxIpsAllowed(rs.getInt("max_ips_allowed"));
        user.setRequiresIPVerification(rs.getBoolean("requires_ip_verification"));

        // Manejo de timestamps (si existen en tu entidad User)
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null && hasCreatedAtField(user)) {
            // user.setCreatedAt(createdAt.toLocalDateTime()); // Si tienes este campo
        }

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null && hasLastLoginField(user)) {
            // user.setLastLogin(lastLogin.toLocalDateTime()); // Si tienes este campo
        }

        return user;
    }

    /**
     * Verifica si la entidad User tiene el campo createdAt
     */
    private boolean hasCreatedAtField(User user) {
        try {
            user.getClass().getMethod("setCreatedAt", LocalDateTime.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Verifica si la entidad User tiene el campo lastLogin
     */
    private boolean hasLastLoginField(User user) {
        try {
            user.getClass().getMethod("setLastLogin", LocalDateTime.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
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