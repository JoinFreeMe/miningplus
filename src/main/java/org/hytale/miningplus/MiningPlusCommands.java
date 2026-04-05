package org.hytale.miningplus;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hytale.miningplus.config.MiningPlusConfig;
import org.hytale.miningplus.ui.MiningPlusConfigPage;

import javax.annotation.Nonnull;

/**
 * {@code /mining+} - Opens the Mining+ configuration UI for the player.
 */
public class MiningPlusCommands extends AbstractPlayerCommand {

    private final MiningPlusConfig config;

    public MiningPlusCommands(MiningPlusConfig config) {
        super("mining+", "Open the Mining+ configuration panel.");
        this.addAliases("m+", "miningplus");
        this.config = config;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        player.getPageManager().openCustomPage(ref, store,
                new MiningPlusConfigPage(playerRef, config));
    }
}
