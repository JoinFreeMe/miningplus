package org.hytale.miningplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hytale.miningplus.system.ExcavationPattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-player configuration for Mining+.
 * Persisted as JSON at {@code mods/MiningPlus/players/{uuid}.json}.
 * Each player gets their own file that survives server restarts.
 */
public class PlayerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private String activationMode = "walk";
    private String pattern = "ORES_ONLY";
    private int maxBlocksPerAction = 64;
    private int maxSearchDepth = 32;
    private boolean silkTouch = false;

    private transient Path configPath;

    /** Loads a player config from disk, or creates a new one with defaults. */
    public static PlayerConfig load(Path path) {
        PlayerConfig config;
        if (Files.exists(path)) {
            try {
                config = GSON.fromJson(Files.readString(path), PlayerConfig.class);
            } catch (IOException e) {
                config = new PlayerConfig();
            }
        } else {
            config = new PlayerConfig();
        }
        config.configPath = path;
        config.save();
        return config;
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

    // --- Getters ---

    public boolean isEnabled() { return enabled; }
    public String getActivationMode() { return activationMode; }
    public int getMaxBlocksPerAction() { return maxBlocksPerAction; }
    public int getMaxSearchDepth() { return maxSearchDepth; }
    public boolean isSilkTouch() { return silkTouch; }

    /** Returns the player's selected excavation pattern, defaulting to ORES_ONLY if invalid. */
    public ExcavationPattern getPattern() {
        try {
            return ExcavationPattern.valueOf(pattern);
        } catch (IllegalArgumentException e) {
            return ExcavationPattern.ORES_ONLY;
        }
    }

    // --- Setters (auto-save) ---

    /** Toggles enabled state. Returns {@code true} if now enabled. */
    public boolean toggleEnabled() {
        enabled = !enabled;
        save();
        return enabled;
    }

    public void setActivationMode(String mode) {
        this.activationMode = mode;
        save();
    }

    public void setPattern(ExcavationPattern pattern) {
        this.pattern = pattern.name();
        save();
    }

    public void setMaxBlocksPerAction(int value) {
        this.maxBlocksPerAction = value;
        save();
    }

    public void setMaxSearchDepth(int value) {
        this.maxSearchDepth = value;
        save();
    }

    public void setSilkTouch(boolean value) {
        this.silkTouch = value;
        save();
    }
}
