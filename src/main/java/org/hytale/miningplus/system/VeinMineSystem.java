package org.hytale.miningplus.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hytale.miningplus.config.MiningPlusConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for {@link BreakBlockEvent} and, depending on the active excavation pattern,
 * either vein-mines connected ores, vein-mines matching blocks, or breaks blocks in a
 * fixed shape. Drops are collected and spawned at the position of the originally mined block.
 */
public class VeinMineSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final String ORE_PREFIX = "Ore";

    @Nonnull
    private static final Query<EntityStore> QUERY = Archetype.of(
            PlayerRef.getComponentType(),
            TransformComponent.getComponentType()
    );

    private final MiningPlusConfig config;
    private final Set<UUID> activeMiners = ConcurrentHashMap.newKeySet();

    public VeinMineSystem(MiningPlusConfig config) {
        super(BreakBlockEvent.class);
        this.config = config;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {

        if (!config.isEnabled()) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ref == null) return;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        UUID playerId = playerRef.getUuid();
        if (playerId == null || !config.isPlayerEnabled(playerId)) return;

        // Check activation key requirement
        String activation = config.getActivationMode(playerId);
        if ("disabled".equals(activation)) return;
        if (!"always".equals(activation)) {
            MovementStatesComponent movementComp = store.getComponent(ref, MovementStatesComponent.getComponentType());
            if (movementComp == null) return;
            MovementStates states = movementComp.getMovementStates();
            if (states == null) return;

            if ("walk".equals(activation) && !states.walking) return;
            if ("crouch".equals(activation) && !states.crouching) return;
        }

        if (!activeMiners.add(playerId)) return;

        try {
            BlockType blockType = event.getBlockType();
            Vector3i origin = event.getTargetBlock();
            if (blockType == null || origin == null) return;

            // Check if the mined block is excluded
            String blockName = blockType.getId();
            if (config.isBlockExcluded(blockName)) return;

            World world = store.getExternalData().getWorld();
            if (world == null) return;

            ExcavationPattern pattern = config.getPattern(playerId);
            int maxBlocks = config.getMaxBlocksPerAction(playerId);
            int maxDepth = config.getMaxSearchDepth(playerId);
            List<Vector3i> targets;

            if (pattern == ExcavationPattern.ORES_ONLY) {
                // Vein mining: only works on ores
                String gatherType = getGatherType(blockType);
                if (gatherType == null || !gatherType.startsWith(ORE_PREFIX)) return;

                targets = FloodFill.search(world, origin,
                        candidate -> !config.isBlockExcluded(candidate.getId())
                                && gatherType.equals(getGatherType(candidate)),
                        maxBlocks, maxDepth);

            } else if (pattern == ExcavationPattern.TARGETED) {
                // Targeted: vein-mine any matching block type
                String targetName = blockType.getId();

                targets = FloodFill.search(world, origin,
                        candidate -> !config.isBlockExcluded(candidate.getId())
                                && targetName.equals(candidate.getId()),
                        maxBlocks, maxDepth);

            } else {
                // Shape-based excavation: works on any block
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) return;

                Vector3d playerPos = transform.getPosition();
                targets = pattern.getPositions(origin, playerPos, maxDepth, maxBlocks);
            }

            if (targets.isEmpty()) return;

            List<ItemStack> drops = new ArrayList<>();

            // Include drop for the origin block (game's own drop gets consumed)
            drops.addAll(resolveDrops(blockType));

            for (Vector3i pos : targets) {
                drops.addAll(removeBlockAndResolveDrops(world, pos));
            }

            Vector3d dropPos = new Vector3d(
                    origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);

            for (ItemStack item : drops) {
                Holder<EntityStore> entity = ItemComponent.generateItemDrop(
                        store, item, dropPos, new Vector3f(0, 0, 0), 0.0F, 0.2F, 0.0F);
                if (entity != null) {
                    commandBuffer.addEntity(entity, AddReason.SPAWN);
                }
            }
        } finally {
            activeMiners.remove(playerId);
        }
    }

    @Nullable
    private static String getGatherType(BlockType blockType) {
        BlockGathering gathering = blockType.getGathering();
        if (gathering == null) return null;
        BlockBreakingDropType breaking = gathering.getBreaking();
        return breaking != null ? breaking.getGatherType() : null;
    }

    private List<ItemStack> removeBlockAndResolveDrops(World world, Vector3i pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) return List.of();

        int localX = pos.getX() & ChunkUtil.SIZE_MASK;
        int localZ = pos.getZ() & ChunkUtil.SIZE_MASK;
        int blockId = chunk.getBlock(localX, pos.getY(), localZ);
        if (blockId == 0) return List.of();

        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null) return List.of();

        // Skip excluded blocks in shape patterns
        if (config.isBlockExcluded(blockType.getId())) return List.of();

        int airId = BlockType.getAssetMap().getIndex("Empty");
        BlockType airType = BlockType.getAssetMap().getAsset(airId);
        chunk.setBlock(localX, pos.getY(), localZ, airId, airType, 0, 0, 256);

        return resolveDrops(blockType);
    }

    private static List<ItemStack> resolveDrops(BlockType blockType) {
        BlockGathering gathering = blockType.getGathering();
        if (gathering != null && gathering.getBreaking() != null) {
            String dropListId = gathering.getBreaking().getDropListId();
            if (dropListId != null) {
                List<ItemStack> drops = ItemModule.get().getRandomItemDrops(dropListId);
                if (!drops.isEmpty()) return drops;

                if (dropListId.startsWith("*")) {
                    drops = ItemModule.get().getRandomItemDrops(dropListId.substring(1));
                    if (!drops.isEmpty()) return drops;
                }
            }
        }

        if (blockType.getItem() != null) {
            return List.of(new ItemStack(blockType.getItem().getId(), 1));
        }

        return List.of();
    }
}
