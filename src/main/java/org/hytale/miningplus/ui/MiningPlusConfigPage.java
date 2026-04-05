package org.hytale.miningplus.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hytale.miningplus.config.MiningPlusConfig;
import org.hytale.miningplus.system.ExcavationPattern;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * In-game configuration page for Mining+.
 * Opened via {@code /mining+}.
 */
public class MiningPlusConfigPage extends InteractiveCustomUIPage<MiningPlusConfigPage.ConfigEvent> {

    private final MiningPlusConfig config;
    private final UUID playerId;

    public MiningPlusConfigPage(PlayerRef playerRef, MiningPlusConfig config) {
        super(playerRef, CustomPageLifetime.CanDismiss, ConfigEvent.CODEC);
        this.config = config;
        this.playerId = playerRef.getUuid();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/MiningPlus/ConfigPage.ui");

        boolean enabled = config.isPlayerEnabled(playerId);
        ExcavationPattern activePattern = config.getPattern(playerId);
        String activationMode = config.getActivationMode(playerId);
        int maxBlocks = config.getMaxBlocksPerAction(playerId);
        int depth = config.getMaxSearchDepth(playerId);

        String activationLabel = switch (activationMode) {
            case "walk" -> "Walk (Alt)";
            case "crouch" -> "Crouch (Ctrl)";
            default -> "None";
        };

        // Status bar
        cmd.set("#ToggleBtn.Text", enabled ? "Disable" : "Enable");
        cmd.set("#StatusLine1.Text", "Status: " + (enabled ? "Enabled" : "Disabled"));
        cmd.set("#StatusLine2.Text", "Mode: " + activePattern.getDisplayName()
                + " | Key: " + activationLabel
                + " | Max: " + maxBlocks + " | Depth: " + depth);
        cmd.set("#MaxBlocksLabel.Text", String.valueOf(maxBlocks));
        cmd.set("#DepthLabel.Text", String.valueOf(depth));

        // Set checkbox states
        cmd.set("#ActivationKeyNone #CheckBox.Value", "always".equals(activationMode));
        cmd.set("#ActivationKeyCrouch #CheckBox.Value", "crouch".equals(activationMode));
        cmd.set("#ActivationKeyWalk #CheckBox.Value", "walk".equals(activationMode));

        // Toggle
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleBtn",
                EventData.of("Action", "toggle"), false);

        // Save activation key button - reads checkbox values
        EventData saveData = new EventData();
        saveData.put("Action", "saveActivation");
        saveData.put("@ActivationKeyNone", "#ActivationKeyNone #CheckBox.Value");
        saveData.put("@ActivationKeyCrouch", "#ActivationKeyCrouch #CheckBox.Value");
        saveData.put("@ActivationKeyWalk", "#ActivationKeyWalk #CheckBox.Value");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SaveActivation", saveData);

        // Pattern buttons
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PatFreeform",
                EventData.of("Action", "pattern").append("Value", "ORES_ONLY"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PatTargeted",
                EventData.of("Action", "pattern").append("Value", "TARGETED"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PatTunnel1x1",
                EventData.of("Action", "pattern").append("Value", "TUNNEL_1x1"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PatTunnel2x1",
                EventData.of("Action", "pattern").append("Value", "TUNNEL_2x1"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PatTunnel3x3",
                EventData.of("Action", "pattern").append("Value", "TUNNEL_3x3"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PatCube3x3",
                EventData.of("Action", "pattern").append("Value", "CUBE_3x3x3"), false);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#PatCube5x5",
                EventData.of("Action", "pattern").append("Value", "CUBE_5x5x5"), false);

        // Max blocks
        for (int n : new int[]{16, 32, 64, 128}) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#MaxBlocks" + n,
                    EventData.of("Action", "maxBlocks").append("Value", String.valueOf(n)), false);
        }
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#MaxBlocksMax",
                EventData.of("Action", "maxBlocks").append("Value", "max"), false);

        // Depth
        for (int n : new int[]{8, 16, 32, 64}) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#Depth" + n,
                    EventData.of("Action", "depth").append("Value", String.valueOf(n)), false);
        }
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DepthMax",
                EventData.of("Action", "depth").append("Value", "max"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ConfigEvent event) {
        if (event.action == null) return;

        switch (event.action) {
            case "toggle" -> {
                config.togglePlayer(playerId);
                rebuild();
            }
            case "saveActivation" -> {
                if (event.activationKeyNone) {
                    config.setActivationMode(playerId, "always");
                } else if (event.activationKeyCrouch) {
                    config.setActivationMode(playerId, "crouch");
                } else if (event.activationKeyWalk) {
                    config.setActivationMode(playerId, "walk");
                } else {
                    config.setActivationMode(playerId, "always");
                }
                rebuild();
            }
            case "pattern" -> {
                if (event.value != null) {
                    try {
                        config.setPattern(playerId, ExcavationPattern.valueOf(event.value));
                    } catch (IllegalArgumentException ignored) {}
                    rebuild();
                }
            }
            case "maxBlocks" -> {
                if (event.value != null) {
                    int value = "max".equals(event.value)
                            ? config.getMaxBlocksPerAction()
                            : Integer.parseInt(event.value);
                    config.setMaxBlocksPerAction(playerId, value);
                    rebuild();
                }
            }
            case "depth" -> {
                if (event.value != null) {
                    int value = "max".equals(event.value)
                            ? config.getMaxSearchDepth()
                            : Integer.parseInt(event.value);
                    config.setMaxSearchDepth(playerId, value);
                    rebuild();
                }
            }
        }
    }

    /** Codec for UI events. */
    public static class ConfigEvent {
        public static final BuilderCodec<ConfigEvent> CODEC = BuilderCodec
                .builder(ConfigEvent.class, ConfigEvent::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Value", Codec.STRING),
                        (o, v) -> o.value = v, o -> o.value).add()
                .append(new KeyedCodec<>("@ActivationKeyNone", Codec.BOOLEAN),
                        (o, v) -> o.activationKeyNone = v, o -> o.activationKeyNone).add()
                .append(new KeyedCodec<>("@ActivationKeyCrouch", Codec.BOOLEAN),
                        (o, v) -> o.activationKeyCrouch = v, o -> o.activationKeyCrouch).add()
                .append(new KeyedCodec<>("@ActivationKeyWalk", Codec.BOOLEAN),
                        (o, v) -> o.activationKeyWalk = v, o -> o.activationKeyWalk).add()
                .build();

        public String action;
        public String value;
        public boolean activationKeyNone;
        public boolean activationKeyCrouch;
        public boolean activationKeyWalk;
    }
}
