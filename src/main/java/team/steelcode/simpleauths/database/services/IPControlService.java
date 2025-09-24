package team.steelcode.simpleauths.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import team.steelcode.simpleauths.entities.IPValidationResult;
import team.steelcode.simpleauths.entities.UserEntity;
import team.steelcode.simpleauths.entities.UserIPHistoryEntity;
import team.steelcode.simpleauths.repositories.UserIPHistoryRepository;
import team.steelcode.simpleauths.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public class IPControlService {

    private final EntityManager entityManager;
    private final UserIPHistoryRepository ipHistoryRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_IP_TRUST_DAYS = 7;
    private static final int MAX_DAILY_IP_CHANGES = 2;
    private static final int DEFAULT_MAX_IPS = 3;

    public IPControlService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.ipHistoryRepository = new UserIPHistoryRepository(entityManager);
        this.userRepository = new UserRepository(entityManager);
    }

    public IPValidationResult validateIPForUser(String uuid, String ipAddress) {
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();

            Optional<UserEntity> userOpt = userRepository.findByUuid(uuid);
            if (!userOpt.isPresent()) {
                transaction.rollback();
                return IPValidationResult.USER_NOT_FOUND;
            }

            UserEntity user = userOpt.get();

            // 1. Verificar si la IP ya está asociada a otra cuenta activa
            List<UserIPHistoryEntity> conflictingIPs = ipHistoryRepository
                    .findActiveUsersByIP(ipAddress, user.getId());

            if (!conflictingIPs.isEmpty()) {
                transaction.rollback();
                return IPValidationResult.IP_ALREADY_IN_USE;
            }

            // 2. Verificar si es una IP conocida del usuario
            Optional<UserIPHistoryEntity> userIPRecordOpt = ipHistoryRepository
                    .findByUserAndIP(user.getId(), ipAddress);

            if (userIPRecordOpt.isPresent()) {
                // IP conocida - actualizar datos
                UserIPHistoryEntity userIPRecord = userIPRecordOpt.get();
                userIPRecord.setLastSeen(LocalDateTime.now());
                userIPRecord.setLoginCount(userIPRecord.getLoginCount() + 1);
                ipHistoryRepository.save(userIPRecord);

                transaction.commit();
                return IPValidationResult.APPROVED;
            }

            // 3. Nueva IP - verificar límites y patrones
            List<UserIPHistoryEntity> userIPs = ipHistoryRepository
                    .findByUserIdOrderByLastSeenDesc(user.getId());

            // Verificar límite de IPs activas (últimos 30 días)
            long activeIPs = userIPs.stream()
                    .filter(ip -> ip.getLastSeen().isAfter(LocalDateTime.now().minusDays(30)))
                    .count();

            int maxAllowed = user.getMaxIpsAllowed() != null ? user.getMaxIpsAllowed() : DEFAULT_MAX_IPS;
            if (activeIPs >= maxAllowed) {
                transaction.rollback();
                return IPValidationResult.TOO_MANY_IPS;
            }

            // Verificar cambios de IP muy frecuentes (últimas 24 horas)
            long recentIPChanges = ipHistoryRepository
                    .countRecentIPsByUser(user.getId(), LocalDateTime.now().minusDays(1));

            if (recentIPChanges >= MAX_DAILY_IP_CHANGES) {
                transaction.rollback();
                return IPValidationResult.TOO_FREQUENT_CHANGES;
            }

            // 4. Crear nuevo registro de IP
            UserIPHistoryEntity newIPRecord = new UserIPHistoryEntity();
            newIPRecord.setUser(user);
            newIPRecord.setIpAddress(ipAddress);
            newIPRecord.setFirstSeen(LocalDateTime.now());
            newIPRecord.setLastSeen(LocalDateTime.now());
            newIPRecord.setLoginCount(1);

            // Marcar como trusted si el usuario tiene buen historial
            boolean shouldTrust = !userIPs.isEmpty() &&
                    userIPs.stream().noneMatch(UserIPHistoryEntity::getBlocked);
            newIPRecord.setTrusted(shouldTrust);
            newIPRecord.setBlocked(false);

            ipHistoryRepository.save(newIPRecord);

            transaction.commit();
            return shouldTrust ? IPValidationResult.APPROVED : IPValidationResult.REQUIRES_VERIFICATION;

        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error validating IP for user: " + uuid, e);
        }
    }

    public void markIPAsTrusted(String uuid, String ipAddress) {
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();

            Optional<UserEntity> userOpt = userRepository.findByUuid(uuid);
            if (!userOpt.isPresent()) {
                transaction.rollback();
                return;
            }

            Optional<UserIPHistoryEntity> ipRecordOpt = ipHistoryRepository
                    .findByUserAndIP(userOpt.get().getId(), ipAddress);

            if (ipRecordOpt.isPresent()) {
                UserIPHistoryEntity ipRecord = ipRecordOpt.get();
                ipRecord.setTrusted(true);
                ipHistoryRepository.save(ipRecord);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error marking IP as trusted", e);
        }
    }

    public void blockIP(String uuid, String ipAddress) {
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();

            Optional<UserEntity> userOpt = userRepository.findByUuid(uuid);
            if (!userOpt.isPresent()) {
                transaction.rollback();
                return;
            }

            Optional<UserIPHistoryEntity> ipRecordOpt = ipHistoryRepository
                    .findByUserAndIP(userOpt.get().getId(), ipAddress);

            if (ipRecordOpt.isPresent()) {
                UserIPHistoryEntity ipRecord = ipRecordOpt.get();
                ipRecord.setBlocked(true);
                ipRecord.setTrusted(false);
                ipHistoryRepository.save(ipRecord);
            }

            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error blocking IP", e);
        }
    }
}