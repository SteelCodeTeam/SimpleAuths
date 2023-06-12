package team.steelcode.simple_auths.data.db.entity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.C;
import org.spongepowered.asm.mixin.Unique;
import team.steelcode.simple_auths.data.LoggedPlayerCache;
import team.steelcode.simple_auths.data.UnloggedPlayerCache;

import javax.persistence.*;
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

    @Transient
    private Vec3 spawnPos;

    @Transient
    private Boolean debuffApplied;


    public PlayerEntityDB() {
        super();
    }

    public PlayerEntityDB(String username, String hashedPassword, UUID uuid) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.uuid = uuid;
    }

    public PlayerEntityDB(String username, String hashedPassword, UUID uuid, Vec3 spawnPos, Boolean debuffAplied) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.uuid = uuid;
        this.spawnPos = spawnPos;
        this.debuffApplied = debuffAplied;
    }

    public static PlayerEntityDB of(Player player) {
        if (LoggedPlayerCache.playerIsLoggedByUUID(player.getUUID())) {
            return LoggedPlayerCache.getPlayerByUUID(player.getUUID());
        } else if (UnloggedPlayerCache.playerIsNotloggedByUUID(player.getUUID())) {
            return UnloggedPlayerCache.getPlayerByUUID(player.getUUID());
        } else {
            return new PlayerEntityDB(player.getScoreboardName(), null, player.getUUID(), player.position(), false);
        }
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

    public Vec3 getSpawnPos() {
        return spawnPos;
    }

    public void setSpawnPos(Vec3 spawnPos) {
        this.spawnPos = spawnPos;
    }

    public Boolean getDebuffApplied() {
        return debuffApplied;
    }

    public void setDebuffApplied(Boolean debuffApplied) {
        this.debuffApplied = debuffApplied;
    }
}
