package team.steelcode.simpleauths.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;

@Entity
@Table(name = "simple_auths_ip_history",
        indexes = {
                @Index(name = "idx_ip_address", columnList = "ip_address"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_first_seen", columnList = "first_seen"),
                @Index(name = "idx_last_seen", columnList = "last_seen")
        }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "simple_auths_ip_history")
public class UserIPHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "ip_address", nullable = false, length = 45) // IPv6 compatible
    private String ipAddress;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "login_count", nullable = false)
    private Integer loginCount = 1;

    @Column(name = "is_trusted", nullable = false)
    private Boolean isTrusted = false;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getBlocked() {
        return isBlocked;
    }

    public void setBlocked(Boolean blocked) {
        isBlocked = blocked;
    }

    public Boolean getTrusted() {
        return isTrusted;
    }

    public void setTrusted(Boolean trusted) {
        isTrusted = trusted;
    }

    public Integer getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}
