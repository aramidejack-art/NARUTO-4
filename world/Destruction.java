package com.mayhem.world;

import com.mayhem.config.MayhemConfig;
import com.mayhem.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Server-side destruction. Everything funnels through {@link #breakAt}, which enforces hardness limits,
 * spawn protection, claim-mod protection (via {@link BlockEvent.BreakEvent}) and the mobGriefing rule.
 * Broken blocks can become short-lived flying debris that shatters mid-air (never re-placed in the world).
 */
public final class Destruction {
    /** Limits for a single move so servers stay smooth. */
    public static final class Budget {
        public int blocks = MayhemConfig.MAX_BLOCKS_PER_MOVE.get();
        public int debris = MayhemConfig.MAX_DEBRIS_PER_MOVE.get();
    }

    private record Debris(UUID id, ResourceKey<Level> dim, int expire, BlockState state) {}

    private static final List<Debris> DEBRIS = new ArrayList<>();

    private Destruction() {}

    public static boolean enabled() {
        return MayhemConfig.DESTRUCTION_ENABLED.get();
    }

    /** Breaks one block if every protection check passes. Returns whether it was removed. */
    public static boolean breakAt(ServerPlayer p, BlockPos pos, double maxHardness, Vec3 velocity, Budget budget) {
        if (!enabled() || budget.blocks <= 0) return false;
        ServerLevel level = p.serverLevel();
        if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) return false;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        Block block = state.getBlock();
        if (block instanceof LiquidBlock || block instanceof DoorBlock || block instanceof BedBlock) return false;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0F || hardness > maxHardness) return false;
        if (state.hasBlockEntity() || state.is(BlockTags.WITHER_IMMUNE)) return false;

        if (MayhemConfig.RESPECT_MOB_GRIEFING.get()
                && !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        if (!level.mayInteract(p, pos)) return false;
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, p))) return false;

        if (level.random.nextDouble() < MayhemConfig.DROP_CHANCE.get()) {
            Block.dropResources(state, level, pos, null, p, ItemStack.EMPTY);
        }

        budget.blocks--;
        if (budget.debris > 0 && velocity != null) {
            budget.debris--;
            FallingBlockEntity debris = FallingBlockEntity.fall(level, pos, state);
            // No gravity: debris flies straight and is discarded mid-air, so it can never land and re-place a block.
            debris.setNoGravity(true);
            debris.setDeltaMovement(velocity);
            debris.hurtMarked = true;
            DEBRIS.add(new Debris(debris.getUUID(), level.dimension(),
                    level.getServer().getTickCount() + 7 + level.random.nextInt(8), state));
        } else {
            level.removeBlock(pos, false);
            Fx.blockDust(level, state, Vec3.atCenterOf(pos), 3);
        }
        return true;
    }

    /** Shatters weak blocks in a forward cone around the player's body (dash strikes, finishers). */
    public static int breakCone(ServerPlayer p, double reach, double halfAngleDeg, double maxHardness, Budget budget) {
        if (!enabled()) return 0;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) return 0;
        flat = flat.normalize();
        double cosLimit = Math.cos(Math.toRadians(halfAngleDeg));
        Vec3 origin = p.position();
        int r = (int) Math.ceil(reach);
        int baseX = Mth.floor(origin.x), baseZ = Mth.floor(origin.z), baseY = Mth.floor(p.getY());

        List<double[]> cands = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double cx = baseX + dx + 0.5 - origin.x;
                double cz = baseZ + dz + 0.5 - origin.z;
                double d = Math.sqrt(cx * cx + cz * cz);
                if (d < 0.6 || d > reach) continue;
                if ((cx / d) * flat.x + (cz / d) * flat.z < cosLimit) continue;
                for (int dy = 0; dy <= 2; dy++) {
                    cands.add(new double[]{baseX + dx, baseY + dy, baseZ + dz, d, cx / d, cz / d});
                }
            }
        }
        cands.sort(Comparator.comparingDouble(a -> a[3]));
        int broken = 0;
        for (double[] c : cands) {
            if (budget.blocks <= 0) break;
            Vec3 vel = new Vec3(c[4] * 0.55, 0.22, c[5] * 0.55);
            if (breakAt(p, new BlockPos((int) c[0], (int) c[1], (int) c[2]), maxHardness, vel, budget)) broken++;
        }
        if (broken > 0) {
            Fx.sound(p.serverLevel(), p.position(), net.minecraft.sounds.SoundEvents.STONE_BREAK, 1.0F, 0.7F);
        }
        return broken;
    }

    /** Digs a bowl-shaped crater centred on {@code center} (surface-following, hardness limited). */
    public static int crater(ServerPlayer p, Vec3 center, double radius, int depth, double maxHardness, Budget budget) {
        if (!enabled()) return 0;
        ServerLevel level = p.serverLevel();
        int cx = Mth.floor(center.x), cz = Mth.floor(center.z), cy = Mth.floor(center.y);
        int r = (int) Math.ceil(radius);

        List<double[]> columns = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= radius) columns.add(new double[]{dx, dz, d});
            }
        }
        columns.sort(Comparator.comparingDouble(a -> a[2]));

        int broken = 0;
        for (double[] col : columns) {
            if (budget.blocks <= 0) break;
            int x = cx + (int) col[0], z = cz + (int) col[1];
            double d = col[2];

            int top = Integer.MIN_VALUE;
            for (int y = cy + 1; y >= cy - 3; y--) {
                BlockState st = level.getBlockState(new BlockPos(x, y, z));
                if (!st.isAir() && !st.canBeReplaced()) {
                    top = y;
                    break;
                }
            }
            if (top == Integer.MIN_VALUE) continue;

            double ox = d < 0.01 ? 0 : col[0] / d, oz = d < 0.01 ? 0 : col[1] / d;
            int columnDepth = Math.max(1, (int) Math.round(depth * (1.0 - d / radius) + 0.4));
            for (int k = 0; k < columnDepth; k++) {
                Vec3 vel = new Vec3(ox * 0.5, 0.35 + 0.1 * k, oz * 0.5);
                if (!breakAt(p, new BlockPos(x, top - k, z), maxHardness, vel, budget)) break;
                broken++;
            }
        }
        return broken;
    }

    /** Crater plus dust ring and heavy sound: the standard "ground shock" used by slams and spikes. */
    public static void shockCrater(ServerPlayer p, Vec3 center, double radius, int depth, double maxHardness) {
        ServerLevel level = p.serverLevel();
        Budget budget = new Budget();
        int broken = crater(p, center, radius, depth, maxHardness, budget);
        BlockPos below = BlockPos.containing(center).below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isAir()) {
            Fx.blockDust(level, ground, center.add(0, 0.2, 0), 30);
        }
        Fx.ring(level, ParticleTypes.CLOUD, center.add(0, 0.15, 0), 0.8, 28, 0.35, 0.02);
        if (broken > 0) {
            Fx.ring(level, ParticleTypes.POOF, center.add(0, 0.3, 0), 1.6, 20, 0.25, 0.05);
        }
    }

    /** Removes debris whose short lifetime is up, shattering it into block particles. */
    public static void tickDebris(MinecraftServer server) {
        if (DEBRIS.isEmpty()) return;
        int now = server.getTickCount();
        Iterator<Debris> it = DEBRIS.iterator();
        int sounds = 0;
        while (it.hasNext()) {
            Debris d = it.next();
            if (now < d.expire()) continue;
            ServerLevel level = server.getLevel(d.dim());
            if (level != null) {
                Entity e = level.getEntity(d.id());
                if (e != null) {
                    Vec3 pos = e.position();
                    e.discard();
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, d.state()),
                            pos.x, pos.y + 0.5, pos.z, 10, 0.3, 0.3, 0.3, 0.1);
                    if (sounds++ < 3) {
                        level.playSound(null, pos.x, pos.y, pos.z, d.state().getSoundType().getBreakSound(),
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 0.9F);
                    }
                }
            }
            it.remove();
        }
    }

    public static void clear() {
        DEBRIS.clear();
    }
}
