package team.steelcode.simpleauths.database.services;

import team.steelcode.simpleauths.database.DatabaseConfig;
import team.steelcode.simpleauths.database.entities.IPValidationResult;
import team.steelcode.simpleauths.database.entities.User;
import team.steelcode.simpleauths.database.entities.UserIPHistory;
import team.steelcode.simpleauths.database.repositories.UserIPHistoryRepository;
import team.steelcode.simpleauths.database.repositories.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IPControlService {

    private static final Logger LOGGER = Logger.getLogger("SimpleAuths");

    private static final int DEFAULT_IP_TRUST_DAYS = 7;
    private static final int MAX_DAILY_IP_CHANGES = 2;
    private static final int DEFAULT_MAX_IPS = 3;
    private static final int MAX_USERS_PER_IP_WARNING = 2;
    private static final int MAX_USERS_PER_IP_BLOCK = 3;
    private static final int MAX_IPS_PER_USER_WARNING = 3;
    private static final int MAX_IPS_PER_USER_BLOCK = 4;

    public IPControlService() {
        // Constructor vacío - utilizamos el pool de conexiones
    }

    /**
     * Valida la IP de un usuario según las reglas del sistema.
     * Método completamente transaccional.
     */
    public IPValidationResult validateUserIP(String userUuid, String ipAddress) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isEmpty()) {
                    conn.rollback();
                    return IPValidationResult.USER_NOT_FOUND;
                }

                User user = userOpt.get();

                // Verificar límite de IPs por usuario
                IPValidationResult ipByUserCheck = checkMultiIPByUser(conn, user.getId(), ipAddress);
                if (ipByUserCheck != IPValidationResult.APPROVED) {
                    conn.rollback();
                    return ipByUserCheck;
                }

                // Verificar límite de usuarios por IP
                IPValidationResult ipByMultiAccountCheck = checkMultiAccountByIP(conn, userUuid, ipAddress);
                if (ipByMultiAccountCheck != IPValidationResult.APPROVED) {
                    conn.rollback();
                    return ipByMultiAccountCheck;
                }

                // Verificar si la IP está bloqueada
                Optional<UserIPHistory> existingIp = ipRepo.findByUserAndIP(user.getId(), ipAddress);
                if (existingIp.isPresent() && existingIp.get().getBlocked()) {
                    conn.rollback();
                    return IPValidationResult.BLOCKED;
                }

                // Verificar si el usuario requiere verificación de IP
                if (user.getRequiresIPVerification() && (existingIp.isEmpty() || !existingIp.get().getTrusted())) {
                    conn.rollback();
                    return IPValidationResult.REQUIRES_VERIFICATION;
                }

                // Verificar cambios frecuentes de IP
                LocalDateTime oneDayAgo = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
                long recentIPCount = ipRepo.countRecentIPsByUser(user.getId(), oneDayAgo);
                if (recentIPCount > MAX_DAILY_IP_CHANGES) {
                    conn.rollback();
                    return IPValidationResult.TOO_FREQUENT_CHANGES;
                }

                // Si llega aquí, crear o actualizar historial de IP
                createOrUpdateIPHistory(conn, user, ipAddress, existingIp.isPresent() && existingIp.get().getTrusted());

                conn.commit();
                return IPValidationResult.APPROVED;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error validando IP de usuario: " + userUuid, e);
            return IPValidationResult.SYSTEM_ERROR;
        }
    }

    /**
     * Marca una IP como confiable para un usuario.
     * Método completamente transaccional.
     */
    public void trustIP(String userUuid, String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Optional<UserIPHistory> ipHistory = ipRepo.findByUserAndIP(user.getId(), ipAddress);

                    if (ipHistory.isPresent()) {
                        UserIPHistory ip = ipHistory.get();
                        ip.setTrusted(true);
                        ip.setBlocked(false);
                        ipRepo.save(ip);
                    } else {
                        createOrUpdateIPHistory(conn, user, ipAddress, true);
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Bloquea una IP para un usuario.
     * Método completamente transaccional.
     */
    public void blockIP(String userUuid, String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Optional<UserIPHistory> ipHistory = ipRepo.findByUserAndIP(user.getId(), ipAddress);

                    if (ipHistory.isPresent()) {
                        UserIPHistory ip = ipHistory.get();
                        ip.setTrusted(false);
                        ip.setBlocked(true);
                        ipRepo.save(ip);
                    } else {
                        UserIPHistory newIp = new UserIPHistory();
                        newIp.setUserId(user.getId());
                        newIp.setIpAddress(ipAddress);
                        newIp.setFirstSeen(LocalDateTime.now());
                        newIp.setLastSeen(LocalDateTime.now());
                        newIp.setLoginCount(1);
                        newIp.setTrusted(false);
                        newIp.setBlocked(true);
                        ipRepo.save(newIp);
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Crear o actualizar registro de historial de IP para un usuario.
     * Método privado transaccional.
     */
    private void createOrUpdateIPHistory(Connection conn, User user, String ipAddress, boolean trusted) throws SQLException {
        UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

        Optional<UserIPHistory> existingIp = ipRepo.findByUserAndIP(user.getId(), ipAddress);

        if (existingIp.isPresent()) {
            // Actualizar existente
            UserIPHistory ip = existingIp.get();
            ip.setLastSeen(LocalDateTime.now());
            ip.setLoginCount(ip.getLoginCount() + 1);
            ip.setTrusted(trusted || ip.getTrusted()); // Mantener el estado de confianza si ya era confiable
            ipRepo.save(ip);
        } else {
            // Crear nuevo
            UserIPHistory newIp = new UserIPHistory();
            newIp.setUserId(user.getId());
            newIp.setIpAddress(ipAddress);
            newIp.setFirstSeen(LocalDateTime.now());
            newIp.setLastSeen(LocalDateTime.now());
            newIp.setLoginCount(1);
            newIp.setTrusted(trusted);
            newIp.setBlocked(false);
            ipRepo.save(newIp);
        }
    }

    /**
     * Verifica si hay demasiados usuarios usando la misma IP.
     * Método privado transaccional.
     */
    private IPValidationResult checkMultiAccountByIP(Connection conn, String userUuid, String ipAddress) throws SQLException {
        // Buscar otros usuarios que usan esta IP
        List<String> excludeUuids = List.of(userUuid);

        UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);
        List<UserIPHistory> otherUsers = ipRepo.findActiveUsersByIP(ipAddress, excludeUuids);

        if (otherUsers.size() >= MAX_USERS_PER_IP_BLOCK) {
            blockAllUsersOnIP(conn, ipAddress);
            return IPValidationResult.MULTI_ACCOUNT_BLOCKED;
        } else if (otherUsers.size() >= MAX_USERS_PER_IP_WARNING) {
            return IPValidationResult.MULTI_ACCOUNT_WARNING;
        }

        return IPValidationResult.APPROVED;
    }

    /**
     * Verifica si un usuario tiene demasiadas IPs.
     * Método privado transaccional.
     */
    private IPValidationResult checkMultiIPByUser(Connection conn, Long userId, String ipAddress) throws SQLException {
        // Verificar cuántas IPs ya tiene este usuario
        UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);
        List<UserIPHistory> userIps = ipRepo.findByUserIdOrderByLastSeenDesc(userId);

        boolean ipExists = userIps.stream()
                .anyMatch(ip -> ip.getIpAddress().equals(ipAddress));

        if (!ipExists) {
            // Solo nos preocupamos si esta IP es nueva
            if (userIps.size() >= MAX_IPS_PER_USER_BLOCK) {
                blockAllIPsForUser(conn, userId);
                return IPValidationResult.TOO_MANY_IPS;
            } else if (userIps.size() >= MAX_IPS_PER_USER_WARNING) {
                return IPValidationResult.TOO_MANY_IPS_WARNING;
            }
        }

        return IPValidationResult.APPROVED;
    }

    /**
     * Bloquea todas las IPs de todos los usuarios que usan una IP específica.
     * Método privado transaccional.
     */
    private void blockAllUsersOnIP(Connection conn, String ipAddress) throws SQLException {
        UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

        // Obtener todos los historiales de IP para esta dirección
        List<UserIPHistory> allIpsForAddress = findAllByIpAddress(conn, ipAddress);

        // Bloquear cada uno
        for (UserIPHistory ip : allIpsForAddress) {
            ip.setBlocked(true);
            ipRepo.save(ip);
        }
    }

    /**
     * Bloquea todas las IPs de un usuario específico.
     * Método privado transaccional.
     */
    private void blockAllIPsForUser(Connection conn, Long userId) throws SQLException {
        UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);
        List<UserIPHistory> userIps = ipRepo.findByUserIdOrderByLastSeenDesc(userId);

        for (UserIPHistory ip : userIps) {
            ip.setBlocked(true);
            ipRepo.save(ip);
        }
    }

    /**
     * Busca todos los registros de historial de IP para una dirección IP específica.
     * Método de utilidad usando JDBC puro.
     */
    private List<UserIPHistory> findAllByIpAddress(Connection conn, String ipAddress) throws SQLException {
        String tableName = DatabaseConfig.getIpHistoryTableName();
        String sql = String.format("SELECT * FROM %s WHERE ip_address = ?", tableName);

        List<UserIPHistory> results = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ipAddress);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserIPHistory ipHistory = new UserIPHistory();
                    ipHistory.setId(rs.getLong("id"));
                    ipHistory.setUserId(rs.getLong("user_id"));
                    ipHistory.setIpAddress(rs.getString("ip_address"));

                    if (rs.getTimestamp("first_seen") != null) {
                        ipHistory.setFirstSeen(rs.getTimestamp("first_seen").toLocalDateTime());
                    }
                    if (rs.getTimestamp("last_seen") != null) {
                        ipHistory.setLastSeen(rs.getTimestamp("last_seen").toLocalDateTime());
                    }

                    ipHistory.setLoginCount(rs.getInt("login_count"));
                    ipHistory.setTrusted(rs.getBoolean("is_trusted"));
                    ipHistory.setBlocked(rs.getBoolean("is_blocked"));

                    results.add(ipHistory);
                }
            }
        }

        return results;
    }

    /**
     * Bloquea globalmente una IP para todos los usuarios
     * Método completamente transaccional.
     */
    public void blockIPGlobally(String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                blockAllUsersOnIP(conn, ipAddress);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Marca como confiable una IP para todos los usuarios que la usan
     * Método completamente transaccional.
     */
    public void trustIPGlobally(String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);
                List<UserIPHistory> allIpsForAddress = findAllByIpAddress(conn, ipAddress);

                for (UserIPHistory ip : allIpsForAddress) {
                    ip.setTrusted(true);
                    ip.setBlocked(false);
                    ipRepo.save(ip);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Encuentra todos los usuarios que utilizan una IP específica
     * Método transaccional.
     */
    public List<UserIPHistory> findAllUsersByIP(String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            return findAllByIpAddress(conn, ipAddress);
        }
    }

    /**
     * Obtiene las estadísticas generales de uso de IP en el sistema
     * Método transaccional.
     */
    public IPStatsResult getIPStats() throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            String ipTableName = DatabaseConfig.getIpHistoryTableName();

            // Contar total de IPs
            long totalIPs = 0;
            String totalSql = String.format("SELECT COUNT(*) FROM %s", ipTableName);
            try (PreparedStatement stmt = conn.prepareStatement(totalSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalIPs = rs.getLong(1);
                }
            }

            // Contar IPs de confianza
            long trustedIPs = 0;
            String trustedSql = String.format("SELECT COUNT(*) FROM %s WHERE is_trusted = ?", ipTableName);
            try (PreparedStatement stmt = conn.prepareStatement(trustedSql)) {
                stmt.setBoolean(1, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        trustedIPs = rs.getLong(1);
                    }
                }
            }

            // Contar IPs bloqueadas
            long blockedIPs = 0;
            String blockedSql = String.format("SELECT COUNT(*) FROM %s WHERE is_blocked = ?", ipTableName);
            try (PreparedStatement stmt = conn.prepareStatement(blockedSql)) {
                stmt.setBoolean(1, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        blockedIPs = rs.getLong(1);
                    }
                }
            }

            // Encontrar IPs con múltiples usuarios (potenciales multi-cuentas)
            List<MultiAccountEntry> multiAccounts = new ArrayList<>();
            String multiAccountSql = String.format(
                    "SELECT ip_address, COUNT(*) as user_count FROM %s " +
                            "GROUP BY ip_address HAVING COUNT(*) > 1 " +
                            "ORDER BY COUNT(*) DESC LIMIT 5",
                    ipTableName
            );

            try (PreparedStatement stmt = conn.prepareStatement(multiAccountSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    multiAccounts.add(new MultiAccountEntry(
                            rs.getString("ip_address"),
                            rs.getInt("user_count")
                    ));
                }
            }

            return new IPStatsResult(totalIPs, trustedIPs, blockedIPs, multiAccounts);
        }
    }

    /**
     * Limpia IPs antiguas no utilizadas (housekeeping)
     */
    public int cleanupOldIPs(int daysThreshold) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            String tableName = DatabaseConfig.getIpHistoryTableName();
            String sql = String.format(
                    "DELETE FROM %s WHERE is_trusted = FALSE AND is_blocked = FALSE " +
                            "AND last_seen < ? AND login_count = 1",
                    tableName
            );

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysThreshold);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, java.sql.Timestamp.valueOf(cutoffDate));
                return stmt.executeUpdate();
            }
        }
    }

    /**
     * Obtiene IPs sospechosas (múltiples usuarios recientes)
     */
    public List<SuspiciousIPResult> getSuspiciousIPs(int minUsers, int daysBack) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            String tableName = DatabaseConfig.getIpHistoryTableName();
            String sql = String.format(
                    "SELECT ip_address, COUNT(DISTINCT user_id) as user_count, " +
                            "MAX(last_seen) as latest_activity " +
                            "FROM %s " +
                            "WHERE last_seen > ? " +
                            "GROUP BY ip_address " +
                            "HAVING COUNT(DISTINCT user_id) >= ? " +
                            "ORDER BY user_count DESC, latest_activity DESC",
                    tableName
            );

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);
            List<SuspiciousIPResult> results = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, java.sql.Timestamp.valueOf(cutoffDate));
                stmt.setInt(2, minUsers);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(new SuspiciousIPResult(
                                rs.getString("ip_address"),
                                rs.getInt("user_count"),
                                rs.getTimestamp("latest_activity").toLocalDateTime()
                        ));
                    }
                }
            }

            return results;
        }
    }

    /**
     * Actualiza la configuración de máximo de IPs permitidas para un usuario
     */
    public void updateUserMaxIPs(String userUuid, int maxIPs) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            Optional<User> userOpt = userRepo.findByPlayername(userUuid);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setMaxIpsAllowed(maxIPs);
                userRepo.save(user);
            } else {
                throw new SQLException("Usuario no encontrado: " + userUuid);
            }
        }
    }

    // ================ CLASES DE RESULTADO ================

    /**
     * Clase para resultados de estadísticas de IP
     */
    public record IPStatsResult(
            long totalIPs,
            long trustedIPs,
            long blockedIPs,
            List<MultiAccountEntry> topMultiAccountIPs) {}

    /**
     * Clase para entradas de multi-cuenta
     */
    public record MultiAccountEntry(String ipAddress, int userCount) {}

    /**
     * Clase para IPs sospechosas
     */
    public record SuspiciousIPResult(
            String ipAddress,
            int userCount,
            LocalDateTime latestActivity) {}
}