package org.hytale.miningplus;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.hytale.miningplus.config.MiningPlusConfig;
import org.hytale.miningplus.system.VeinMineSystem;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Mining+ - Vein mining and excavation patterns for Hytale.
 * Break an ore to mine the entire vein, or use tunnel/cube patterns to excavate.
 * Configure via {@code /mining+} or sneak + right-click with a pickaxe to cycle modes.
 */
public class MiningPlusPlugin extends JavaPlugin {

    private MiningPlusConfig config;

    public MiningPlusPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    public CompletableFuture<Void> preLoad() {
        new MiningPlusAssets(modDirectory(), getLogger(), getClass()).extractAll();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void setup() {
        config = MiningPlusConfig.load(modDirectory().resolve("config.json"));

        getEntityStoreRegistry().registerSystem(new VeinMineSystem(config));
        getCommandRegistry().registerCommand(new MiningPlusCommands(config));

        getLogger().atInfo().log("[MiningPlus] v1.0.0 loaded - max blocks: %d, search depth: %d",
                config.getMaxBlocksPerAction(), config.getMaxSearchDepth());
    }

    private Path modDirectory() {
        return getDataDirectory().resolveSibling("MiningPlus");
    }
}
