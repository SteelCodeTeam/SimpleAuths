package team.steelcode.simpleauths.database.entities;

import java.util.HashSet;
import java.util.Set;
/**
 * POJO User entity - Sin anotaciones JPA para jOOQ
 */
public class User {
    private Long id;
    private String uuid;
    private String username;
    private String password;
    private String email;
    private Long playTime;
    private Integer maxIpsAllowed;
    private Boolean requiresIPVerification;
    private Set<UserIPHistory> ipHistory = new HashSet<>();

    // Constructores
    public User() {
        this.playTime = 0L;
        this.requiresIPVerification = false;
        this.maxIpsAllowed = 3;
        this.ipHistory = new HashSet<>();
    }

    public User(Long id, String uuid, String username, String password) {
        this.id = id;
        this.uuid = uuid;
        this.username = username;
        this.password = password;
        this.playTime = 0L;
        this.requiresIPVerification = false;
        this.maxIpsAllowed = 3;
        this.ipHistory = new HashSet<>();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getPlayTime() { return playTime; }
    public void setPlayTime(Long playTime) { this.playTime = playTime; }

    public Integer getMaxIpsAllowed() { return maxIpsAllowed; }
    public void setMaxIpsAllowed(Integer maxIpsAllowed) { this.maxIpsAllowed = maxIpsAllowed; }

    public Boolean getRequiresIPVerification() { return requiresIPVerification; }
    public void setRequiresIPVerification(Boolean requiresIPVerification) { this.requiresIPVerification = requiresIPVerification; }

    public Set<UserIPHistory> getIpHistory() { return ipHistory; }
    public void setIpHistory(Set<UserIPHistory> ipHistory) { this.ipHistory = ipHistory; }
}
