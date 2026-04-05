package org.hytale.miningplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hytale.miningplus.system.ExcavationPattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private int maxBlocksPerAction = 128;
    private int maxSearchDepth = 64;
    private List<String> excludedBlocks = List.of("bedrock", "rock_bedrock");
    private boolean allowSilkTouch = false;

    private transient Path configPath;
    private transient Set<String> excludedBlockSet;
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
        config.rebuildExclusionSet();
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
        this.excludedBlocks = fresh.excludedBlocks;
        this.allowSilkTouch = fresh.allowSilkTouch;
        rebuildExclusionSet();
    }

    private void rebuildExclusionSet() {
        excludedBlockSet = new HashSet<>();
        if (excludedBlocks != null) {
            for (String name : excludedBlocks) {
                excludedBlockSet.add(name.toLowerCase());
            }
        }
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

    /** Checks whether a block name is on the server exclusion list. Case-insensitive. */
    public boolean isBlockExcluded(String blockName) {
        if (excludedBlockSet == null || blockName == null) return false;
        return excludedBlockSet.contains(blockName.toLowerCase());
    }

    public boolean isAllowSilkTouch() { return allowSilkTouch; }

    /** Returns true if the player has silk touch enabled and the server allows it. */
    public boolean isSilkTouch(UUID playerId) {
        return allowSilkTouch && getPlayerConfig(playerId).isSilkTouch();
    }

    public void setSilkTouch(UUID playerId, boolean value) {
        getPlayerConfig(playerId).setSilkTouch(value);
    }

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
