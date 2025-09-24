package team.steelcode.simpleauths.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "simple_auths_user_entity",
        indexes = {
                @Index(name = "idx_player_uuid", columnList = "uuid", unique = true),
                @Index(name = "idx_player_name", columnList = "username"),
                @Index(name = "idx_player_active", columnList = "is_active"),
                @Index(name = "idx_player_last_seen", columnList = "last_seen")
        }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "simple_auths_user_entity")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "player_id",  nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "username", nullable = false, length = 16)
    private String username;

    @Column(name = "password", nullable = false, length = 16)
    private String password;

    @Column(name = "email", length = 32)
    private String email;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "play_time", nullable = false)
    private Long playTime = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "max_ips_allowed", nullable = false)
    private Integer maxIpsAllowed = 3; // Configurable por usuario

    @Column(name = "requires_ip_verification", nullable = false)
    private Boolean requiresIPVerification = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserIPHistoryEntity> ipHistory = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Long getPlayTime() {
        return playTime;
    }

    public void setPlayTime(Long playTime) {
        this.playTime = playTime;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Integer getMaxIpsAllowed() {
        return maxIpsAllowed;
    }

    public void setMaxIpsAllowed(Integer maxIpsAllowed) {
        this.maxIpsAllowed = maxIpsAllowed;
    }

    public Boolean getRequiresIPVerification() {
        return requiresIPVerification;
    }

    public void setRequiresIPVerification(Boolean requiresIPVerification) {
        this.requiresIPVerification = requiresIPVerification;
    }

    public Set<UserIPHistoryEntity> getIpHistory() {
        return ipHistory;
    }

    public void setIpHistory(Set<UserIPHistoryEntity> ipHistory) {
        this.ipHistory = ipHistory;
    }
}
