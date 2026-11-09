package com.mayhem.shinobi.registry;

import com.mayhem.shinobi.data.ShinobiData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin film of chakra that stands in for the top water block under a water-walker (the same idea as Frost Walker's frosted
 * ice). It is a real solid block, so the server and the client agree you are standing on something and no "flying" kick fires.
 * It schedules its own tick and turns back into water as soon as no walker is near, so it self-heals even after a crash.
 */
public class ChakraSurfaceBlock extends Block {
    // 14/16 tall: the surface of a water source block is ~0.89, so the walker stands right on the water line.
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public ChakraSurfaceBlock() {
        super(Properties.of().mapColor(MapColor.WATER).noLootTable().noOcclusion().strength(-1.0F, 3600000.0F)
                .sound(SoundType.GLASS).pushReaction(PushReaction.DESTROY)
                .isSuffocating((s, g, p) -> false).isViewBlocking((s, g, p) -> false));
    }

    @Override public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 6);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        AABB box = new AABB(pos).inflate(1.6, 2.0, 1.6);
        boolean walker = false;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (ShinobiData.of(p).getBoolean("waterWalk") && !p.isShiftKeyDown()) { walker = true; break; }
        }
        if (walker) level.scheduleTick(pos, this, 4);
        else level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
    }
}
