package team.steelcode.simple_auths.data.db.entity;

import org.spongepowered.asm.mixin.Unique;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.UUID;

@Entity(name = "player_entity_table")
public class PlayerEntityDB implements Serializable {

    @GeneratedValue
    @Id
    @Column(name = "id")
    private long id;

    @Unique(silent = true)
    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String hashedPassword;

    @Column(name = "uuid")
    private UUID uuid;

    public PlayerEntityDB() {

    }

    public PlayerEntityDB(String username, String hashedPassword, UUID uuid) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.uuid = uuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
