package org.hytale.miningplus;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Extracts bundled assets (UI files, manifest) from the JAR to the mod data directory
 * so the server can load them. Called during {@code preLoad()}.
 */
public final class MiningPlusAssets {

    private final Path dataPath;
    private final HytaleLogger logger;
    private final Class<?> resourceLoader;

    public MiningPlusAssets(Path dataPath, HytaleLogger logger, Class<?> resourceLoader) {
        this.dataPath = dataPath;
        this.logger = logger;
        this.resourceLoader = resourceLoader;
    }

    public void extractAll() {
        extract("/manifest.json", "manifest.json");
        extract("/Common/UI/Custom/Pages/MiningPlus/ConfigPage.ui",
                "Common/UI/Custom/Pages/MiningPlus/ConfigPage.ui");
    }

    private void extract(String resourcePath, String destRelative) {
        Path dest = dataPath.resolve(destRelative);
        try (InputStream in = resourceLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.atWarning().log("[MiningPlus] Bundled resource not found: %s", resourcePath);
                return;
            }
            Files.createDirectories(dest.getParent());
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            logger.atWarning().log("[MiningPlus] Failed to extract %s: %s", resourcePath, e.getMessage());
        }
    }
}
