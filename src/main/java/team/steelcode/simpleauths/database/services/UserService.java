package team.steelcode.simpleauths.database.services;

import team.steelcode.simpleauths.database.DatabaseConfig;
import team.steelcode.simpleauths.database.entities.User;
import team.steelcode.simpleauths.database.entities.UserIPHistory;
import team.steelcode.simpleauths.database.repositories.UserRepository;
import team.steelcode.simpleauths.database.repositories.UserIPHistoryRepository;
import team.steelcode.simpleauths.database.util.UserStats;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class UserService {

    private static final Logger LOGGER = Logger.getLogger("SimpleAuths");

    public UserService() {
        // Constructor vacío - utilizamos el pool de conexiones
    }

    /**
     * Crea un nuevo usuario con los parámetros especificados
     */
    public User createUser(String username, String password) throws SQLException {
        return createUser(username, password, null); // Email opcional
    }

    /**
     * Crea un nuevo usuario con username, password y email
     */
    public User createUser(String username, String password, String email) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);

                // Verificar si el usuario ya existe
                if (userRepo.existsByUsername(username)) {
                    throw new SQLException("El nombre de usuario ya existe: " + username);
                }

                User user = new User();
                user.setUuid(UUID.randomUUID().toString());
                user.setUsername(username);
                user.setPassword(password);
                user.setEmail(email != null ? email : "");
                user.setPlayTime(0L);
                user.setMaxIpsAllowed(3);
                user.setRequiresIPVerification(false);

                User savedUser = userRepo.save(user);
                conn.commit();

                return savedUser;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Registra una nueva IP para un usuario (transaccional)
     */
    public UserIPHistory registerUserIP(String userUuid, String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

                // Buscar usuario dentro de la transacción
                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isEmpty()) {
                    throw new SQLException("Usuario no encontrado: " + userUuid);
                }

                User user = userOpt.get();

                // Buscar registro de IP existente
                Optional<UserIPHistory> ipHistoryOpt = ipRepo.findByUserAndIP(user.getId(), ipAddress);

                UserIPHistory ipHistory;
                if (ipHistoryOpt.isPresent()) {
                    // Actualizar registro existente
                    ipHistory = ipHistoryOpt.get();
                    ipHistory.setLastSeen(LocalDateTime.now());
                    ipHistory.setLoginCount(ipHistory.getLoginCount() + 1);
                } else {
                    // Crear nuevo registro
                    ipHistory = new UserIPHistory();
                    ipHistory.setUserId(user.getId());
                    ipHistory.setIpAddress(ipAddress);
                    ipHistory.setFirstSeen(LocalDateTime.now());
                    ipHistory.setLastSeen(LocalDateTime.now());
                    ipHistory.setLoginCount(1);
                    ipHistory.setTrusted(false);
                    ipHistory.setBlocked(false);
                }

                ipRepo.save(ipHistory);
                conn.commit();

                return ipHistory;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Encuentra un usuario por su UUID
     */
    public Optional<User> findUserByUuid(String uuid) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            return userRepo.findByPlayername(uuid);
        }
    }

    /**
     * Encuentra un usuario por su nombre de usuario
     */
    public Optional<User> findUserByUsername(String username) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            return userRepo.findByUsername(username);
        }
    }

    /**
     * Actualiza información del usuario
     */
    public void updateUser(User user) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            userRepo.save(user);
        }
    }

    /**
     * Actualiza el tiempo de juego del usuario
     */
    public void updatePlayTime(String userUuid, long additionalTime) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setPlayTime(user.getPlayTime() + additionalTime);
                    userRepo.save(user);
                    conn.commit();
                } else {
                    throw new SQLException("Usuario no encontrado: " + userUuid);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Establece el número máximo de IPs permitidas para un usuario
     */
    public void setMaxIpsAllowed(String userUuid, int maxIps) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setMaxIpsAllowed(maxIps);
                    userRepo.save(user);
                    conn.commit();
                } else {
                    throw new SQLException("Usuario no encontrado: " + userUuid);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Habilita o deshabilita el requerimiento de verificación IP para un usuario
     */
    public void setRequiresIPVerification(String userUuid, boolean requiresVerification) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setRequiresIPVerification(requiresVerification);
                    userRepo.save(user);
                    conn.commit();
                } else {
                    throw new SQLException("Usuario no encontrado: " + userUuid);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Cambia la contraseña del usuario
     */
    public boolean changePassword(String userUuid, String oldPassword, String newPassword) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getPassword().equals(oldPassword)) {
                        user.setPassword(newPassword);
                        userRepo.save(user);
                        conn.commit();
                        return true;
                    }
                }
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Cambia la contraseña sin verificar la anterior (para administradores)
     */
    public void resetPassword(String userUuid, String newPassword) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            userRepo.updatePassword(getUserId(userUuid), newPassword);
        }
    }

    /**
     * Actualiza el email del usuario
     */
    public void updateEmail(String userUuid, String newEmail) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setEmail(newEmail);
                    userRepo.save(user);
                    conn.commit();
                } else {
                    throw new SQLException("Usuario no encontrado: " + userUuid);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Actualiza la última conexión del usuario
     */
    public void updateLastLogin(String userUuid) throws SQLException {
        updateLastLogin(userUuid, LocalDateTime.now());
    }

    /**
     * Actualiza la última conexión del usuario con fecha específica
     */
    public void updateLastLogin(String userUuid, LocalDateTime loginTime) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            userRepo.updateLastLogin(getUserId(userUuid), loginTime);
        }
    }

    /**
     * Obtiene todo el historial de IPs de un usuario
     */
    public List<UserIPHistory> getUserIPHistory(String userUuid) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

            Optional<User> userOpt = userRepo.findByPlayername(userUuid);
            if (userOpt.isPresent()) {
                return ipRepo.findByUserIdOrderByLastSeenDesc(userOpt.get().getId());
            }
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene estadísticas del usuario incluyendo tiempo total de juego y conteo de IPs
     */
    public UserStats getUserStats(String userUuid) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

            Optional<User> userOpt = userRepo.findByPlayername(userUuid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                List<UserIPHistory> ipHistory = ipRepo.findByUserIdOrderByLastSeenDesc(user.getId());

                long totalIPs = ipHistory.size();
                long trustedIPs = ipHistory.stream().mapToLong(ip -> ip.getTrusted() ? 1 : 0).sum();
                long blockedIPs = ipHistory.stream().mapToLong(ip -> ip.getBlocked() ? 1 : 0).sum();

                return new UserStats(
                        user.getPlayTime(),
                        totalIPs,
                        trustedIPs,
                        blockedIPs,
                        user.getMaxIpsAllowed(),
                        user.getRequiresIPVerification()
                );
            }
            return null;
        }
    }

    /**
     * Bloquea una IP de usuario (transaccional)
     */
    public boolean blockUserIP(String userUuid, String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isEmpty()) {
                    conn.rollback();
                    return false;
                }

                Optional<UserIPHistory> ipOpt = ipRepo.findByUserAndIP(userOpt.get().getId(), ipAddress);
                if (ipOpt.isEmpty()) {
                    conn.rollback();
                    return false;
                }

                UserIPHistory ip = ipOpt.get();
                ip.setBlocked(true);
                ipRepo.save(ip);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Marca como confiable una IP de usuario (transaccional)
     */
    public boolean trustUserIP(String userUuid, String ipAddress) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isEmpty()) {
                    conn.rollback();
                    return false;
                }

                Optional<UserIPHistory> ipOpt = ipRepo.findByUserAndIP(userOpt.get().getId(), ipAddress);
                if (ipOpt.isEmpty()) {
                    conn.rollback();
                    return false;
                }

                UserIPHistory ip = ipOpt.get();
                ip.setTrusted(true);
                ipRepo.save(ip);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Valida las credenciales del usuario
     */
    public boolean validateCredentials(String playerName, String password) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            Optional<User> userOpt = userRepo.findByPlayername(playerName);
            return userOpt.isPresent() && userOpt.get().getPassword().equals(password);
        }
    }

    /**
     * Valida credenciales por nombre de usuario
     */
    public boolean validateCredentialsByUsername(String username, String password) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            Optional<User> userOpt = userRepo.findByUsername(username);
            return userOpt.isPresent() && userOpt.get().getPassword().equals(password);
        }
    }

    /**
     * Verifica si existe un usuario
     */
    public boolean userExists(String username) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            return userRepo.existsByUsername(username);
        }
    }

    /**
     * Verifica si existe un usuario por nombre de usuario
     */
    public boolean usernameExists(String username) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            return userRepo.existsByUsername(username);
        }
    }

    /**
     * Elimina un usuario y todo su historial
     */
    public boolean deleteUser(String userUuid) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);

            try {
                UserRepository userRepo = new UserRepository(conn);
                UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

                Optional<User> userOpt = userRepo.findByPlayername(userUuid);
                if (userOpt.isEmpty()) {
                    conn.rollback();
                    return false;
                }

                Long userId = userOpt.get().getId();

                // Eliminar historial IP primero (debido a foreign key)
                ipRepo.deleteByUserId(userId);

                // Eliminar usuario
                boolean deleted = userRepo.deleteByUuid(userUuid);

                if (deleted) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Obtiene una lista paginada de usuarios
     */
    public List<User> getAllUsers(int limit, int offset) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            return userRepo.findAll(limit, offset);
        }
    }

    /**
     * Cuenta el total de usuarios registrados
     */
    public long getTotalUserCount() throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            return userRepo.countAll();
        }
    }

    /**
     * Busca usuarios por email
     */
    public List<User> findUsersByEmail(String email) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            return userRepo.findByEmail(email);
        }
    }

    /**
     * Obtiene las IPs bloqueadas de un usuario
     */
    public List<UserIPHistory> getUserBlockedIPs(String userUuid) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

            Optional<User> userOpt = userRepo.findByPlayername(userUuid);
            if (userOpt.isPresent()) {
                return ipRepo.findByUserIdOrderByLastSeenDesc(userOpt.get().getId())
                        .stream()
                        .filter(UserIPHistory::getBlocked)
                        .toList();
            }
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene las IPs confiables de un usuario
     */
    public List<UserIPHistory> getUserTrustedIPs(String userUuid) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            UserIPHistoryRepository ipRepo = new UserIPHistoryRepository(conn);

            Optional<User> userOpt = userRepo.findByPlayername(userUuid);
            if (userOpt.isPresent()) {
                return ipRepo.findByUserIdOrderByLastSeenDesc(userOpt.get().getId())
                        .stream()
                        .filter(UserIPHistory::getTrusted)
                        .toList();
            }
            return new ArrayList<>();
        }
    }

    // ================ MÉTODOS PRIVADOS AUXILIARES ================

    /**
     * Obtiene el ID interno del usuario a partir del UUID
     */
    private Long getUserId(String userUuid) throws SQLException {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            UserRepository userRepo = new UserRepository(conn);
            Optional<User> userOpt = userRepo.findByPlayername(userUuid);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
            throw new SQLException("Usuario no encontrado: " + userUuid);
        }
    }

    /**
     * Limpia la contraseña de un objeto User para logging seguro
     */
    private User sanitizeUserForLogging(User user) {
        User sanitized = new User();
        sanitized.setId(user.getId());
        sanitized.setUuid(user.getUuid());
        sanitized.setUsername(user.getUsername());
        sanitized.setPassword("***");
        sanitized.setEmail(user.getEmail());
        sanitized.setPlayTime(user.getPlayTime());
        sanitized.setMaxIpsAllowed(user.getMaxIpsAllowed());
        sanitized.setRequiresIPVerification(user.getRequiresIPVerification());
        return sanitized;
    }
}