package team.steelcode.simpleauths.repositories;

import team.steelcode.simpleauths.entities.UserIPHistoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserIPHistoryRepository {
    private final EntityManager entityManager;

    public UserIPHistoryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void save(UserIPHistoryEntity ipHistory) {
        if (ipHistory.getId() == null) {
            entityManager.persist(ipHistory);
        } else {
            entityManager.merge(ipHistory);
        }
    }

    public Optional<UserIPHistoryEntity> findByUserAndIP(Long userId, String ipAddress) {
        TypedQuery<UserIPHistoryEntity> query = entityManager.createQuery(
                "SELECT ip FROM UserIPHistoryEntity ip WHERE ip.user.id = :userId AND ip.ipAddress = :ipAddress",
                UserIPHistoryEntity.class
        );
        query.setParameter("userId", userId);
        query.setParameter("ipAddress", ipAddress);

        List<UserIPHistoryEntity> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<UserIPHistoryEntity> findActiveUsersByIP(String ipAddress, Long excludeUserId) {
        TypedQuery<UserIPHistoryEntity> query = entityManager.createQuery(
                "SELECT ip FROM UserIPHistoryEntity ip " +
                        "WHERE ip.ipAddress = :ipAddress " +
                        "AND ip.user.id != :excludeUserId " +
                        "AND ip.user.isActive = true " +
                        "AND ip.isBlocked = false " +
                        "AND ip.lastSeen > :cutoffDate",
                UserIPHistoryEntity.class
        );
        query.setParameter("ipAddress", ipAddress);
        query.setParameter("excludeUserId", excludeUserId);
        query.setParameter("cutoffDate", LocalDateTime.now().minusDays(30));

        return query.getResultList();
    }

    public List<UserIPHistoryEntity> findByUserIdOrderByLastSeenDesc(Long userId) {
        TypedQuery<UserIPHistoryEntity> query = entityManager.createQuery(
                "SELECT ip FROM UserIPHistoryEntity ip " +
                        "WHERE ip.user.id = :userId " +
                        "ORDER BY ip.lastSeen DESC",
                UserIPHistoryEntity.class
        );
        query.setParameter("userId", userId);

        return query.getResultList();
    }

    public long countRecentIPsByUser(Long userId, LocalDateTime since) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(ip) FROM UserIPHistoryEntity ip " +
                        "WHERE ip.user.id = :userId AND ip.firstSeen > :since",
                Long.class
        );
        query.setParameter("userId", userId);
        query.setParameter("since", since);

        return query.getSingleResult();
    }
}