package team.steelcode.simpleauths.database.entities;

import team.steelcode.simpleauths.database.entities.User;
import java.time.LocalDateTime;

public class UserIPHistory {
    private Long id;
    private Long userId;
    private String ipAddress;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private Integer loginCount;
    private Boolean isTrusted = false;
    private Boolean isBlocked = false;

    // Referencia al usuario (para mantener compatibilidad)
    private User user;

    public UserIPHistory() {}

    public UserIPHistory(Long id, Boolean isBlocked, Boolean isTrusted, Integer loginCount,
                         LocalDateTime lastSeen, String ipAddress, LocalDateTime firstSeen, Long userId) {
        this.id = id;
        this.isBlocked = isBlocked;
        this.isTrusted = isTrusted;
        this.loginCount = loginCount;
        this.lastSeen = lastSeen;
        this.ipAddress = ipAddress;
        this.firstSeen = firstSeen;
        this.userId = userId;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public Integer getLoginCount() { return loginCount; }
    public void setLoginCount(Integer loginCount) { this.loginCount = loginCount; }

    public Boolean getTrusted() { return isTrusted; }
    public void setTrusted(Boolean trusted) { isTrusted = trusted; }

    public Boolean getBlocked() { return isBlocked; }
    public void setBlocked(Boolean blocked) { isBlocked = blocked; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
