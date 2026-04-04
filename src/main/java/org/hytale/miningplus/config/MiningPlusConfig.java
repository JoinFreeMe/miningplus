package org.hytale.miningplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hytale.miningplus.system.ExcavationPattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-wide configuration for Mining+.
 * Persisted as JSON at {@code mods/MiningPlus/config.json}.
 *
 * <p>Server settings define the upper limits for per-player values.
 * Per-player settings are stored in {@code mods/MiningPlus/players/{uuid}.json}
 * and are always capped at the server maximum.
 */
public class MiningPlusConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private int maxBlocksPerAction = 64;
    private int maxSearchDepth = 32;

    private transient Path configPath;
    private transient Path playersDir;
    private transient Map<UUID, PlayerConfig> playerConfigs = new ConcurrentHashMap<>();

    /** Loads the server config from disk, or creates a new one with defaults. */
    public static MiningPlusConfig load(Path path) {
        MiningPlusConfig config;
        if (Files.exists(path)) {
            try {
                config = GSON.fromJson(Files.readString(path), MiningPlusConfig.class);
            } catch (IOException e) {
                config = new MiningPlusConfig();
            }
        } else {
            config = new MiningPlusConfig();
        }
        config.configPath = path;
        config.playersDir = path.getParent().resolve("players");
        config.playerConfigs = new ConcurrentHashMap<>();
        config.save();
        return config;
    }

    /** Reloads the server config from disk without affecting player configs. */
    public void reload() {
        if (configPath == null) return;
        MiningPlusConfig fresh = load(configPath);
        this.enabled = fresh.enabled;
        this.maxBlocksPerAction = fresh.maxBlocksPerAction;
        this.maxSearchDepth = fresh.maxSearchDepth;
    }

    private void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException ignored) {
            // Config save failures are non-fatal
        }
    }

    // --- Server-wide settings (upper limits) ---

    public boolean isEnabled() { return enabled; }
    public int getMaxBlocksPerAction() { return maxBlocksPerAction; }
    public int getMaxSearchDepth() { return maxSearchDepth; }

    // --- Per-player config access ---

    private PlayerConfig getPlayerConfig(UUID playerId) {
        return playerConfigs.computeIfAbsent(playerId, id ->
                PlayerConfig.load(playersDir.resolve(id.toString() + ".json")));
    }

    public boolean isPlayerEnabled(UUID playerId) {
        return getPlayerConfig(playerId).isEnabled();
    }

    public boolean togglePlayer(UUID playerId) {
        return getPlayerConfig(playerId).toggleEnabled();
    }

    public ExcavationPattern getPattern(UUID playerId) {
        return getPlayerConfig(playerId).getPattern();
    }

    public void setPattern(UUID playerId, ExcavationPattern pattern) {
        getPlayerConfig(playerId).setPattern(pattern);
    }

    public String getActivationMode(UUID playerId) {
        return getPlayerConfig(playerId).getActivationMode();
    }

    public void setActivationMode(UUID playerId, String mode) {
        getPlayerConfig(playerId).setActivationMode(mode);
    }

    /** Returns the player's max blocks, capped at the server maximum. */
    public int getMaxBlocksPerAction(UUID playerId) {
        return Math.min(getPlayerConfig(playerId).getMaxBlocksPerAction(), maxBlocksPerAction);
    }

    /** Sets the player's max blocks, capped at the server maximum. */
    public void setMaxBlocksPerAction(UUID playerId, int value) {
        getPlayerConfig(playerId).setMaxBlocksPerAction(Math.min(value, maxBlocksPerAction));
    }

    /** Returns the player's search depth, capped at the server maximum. */
    public int getMaxSearchDepth(UUID playerId) {
        return Math.min(getPlayerConfig(playerId).getMaxSearchDepth(), maxSearchDepth);
    }

    /** Sets the player's search depth, capped at the server maximum. */
    public void setMaxSearchDepth(UUID playerId, int value) {
        getPlayerConfig(playerId).setMaxSearchDepth(Math.min(value, maxSearchDepth));
    }
}
