package com.mayhem.shinobi.move;

import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Water walking and wall climbing.
 * WATER: every tick the surface water blocks around the player's feet become solid chakra film (ChakraSurfaceBlock), which
 * turns back to water when you leave, sneak, or run out of chakra. It is a real block, so it works identically on server and
 * client with no prediction tricks and no anti-fly kicks. The client additionally pops you up to the surface if you fall in.
 * WALL: the client climbs (ClientTraversal); the server resets fall damage and drains chakra.
 */
public final class Traversal {
    private Traversal() {}

    public static boolean onWater(ServerPlayer p) {
        BlockPos feet = p.blockPosition();
        return p.level().getFluidState(feet).is(FluidTags.WATER) || p.level().getFluidState(feet.below()).is(FluidTags.WATER);
    }

    public static void tick(ServerPlayer p, CompoundTag t) {
        boolean water = t.getBoolean("waterWalk"), wall = t.getBoolean("wallWalk");
        if (!water && !wall) return;
        long now = p.server.getTickCount();
        boolean on = false, onWaterNow = false, onWallNow = false;
        int drain = 0;
        ServerLevel sl = p.serverLevel();
        if (water && !p.isShiftKeyDown() && !p.isSwimming() && !p.isPassenger()) {
            if (placeSurface(sl, p)) {
                p.fallDistance = 0;
                drain += ShinobiConfig.WATER_DRAIN_PER_SEC.get();
                on = true; onWaterNow = true;
                if (now % 4 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 0.002)
                    sl.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.05, p.getZ(), 4, 0.25, 0.02, 0.25, 0.1);
                if (now % 10 == 0) sl.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 0.02, p.getZ(), 3, 0.3, 0.0, 0.3, 0.0);
            }
        }
        if (wall && p.horizontalCollision && !p.onGround() && !p.isInWater()) {
            p.fallDistance = 0;
            drain += ShinobiConfig.WALL_DRAIN_PER_SEC.get();
            on = true; onWallNow = true;
        }
        if (now % 20 == 0) {
            if (onWaterNow) com.mayhem.shinobi.story.MissionTracker.onTraverse(p, "water");
            if (onWallNow) com.mayhem.shinobi.story.MissionTracker.onTraverse(p, "wall");
        }
        if (on && now % 20 == 0 && drain > 0 && !Resources.spendChakra(p, t, drain)) {
            t.putBoolean("waterWalk", false);
            t.putBoolean("wallWalk", false);
            p.displayClientMessage(Component.literal("Out of chakra - surface walking ended"), true);
        }
    }

    /** Converts the water-surface blocks in a 5x5 around the feet into chakra film. Returns true if the player is on/over water. */
    private static boolean placeSurface(ServerLevel sl, ServerPlayer p) {
        BlockPos base = p.blockPosition();
        boolean any = false;
        BlockState film = ShinobiRegistry.CHAKRA_SURFACE.get().defaultBlockState();
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(pos);
                    if (st.is(ShinobiRegistry.CHAKRA_SURFACE.get())) { if (dx == 0 && dz == 0) any = true; continue; }
                    if (st.is(Blocks.WATER) && st.getFluidState().isSource() && sl.getBlockState(pos.above()).isAir()) {
                        sl.setBlock(pos, film, 3);
                        if (dx == 0 && dz == 0) any = true;
                    }
                }
            }
        }
        return any;
    }
}
