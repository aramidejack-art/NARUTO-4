package com.mayhem.fx;

import com.mayhem.combat.MoveType;
import com.mayhem.network.ImpactPacket;
import com.mayhem.network.MayhemNetwork;
import com.mayhem.network.MoveAnimPacket;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Server-side effect helpers: animations, camera impact, particles and sounds. */
public final class Fx {
    private Fx() {}

    // ---- comic text ids (indexes into the client's text table) ----
    public static final byte TXT_NONE = 0, TXT_WHAM = 1, TXT_BAM = 2, TXT_DON = 3, TXT_ORA = 4,
            TXT_BOOM = 5, TXT_RUMBLE = 6, TXT_CRACK = 7, TXT_ZAP = 8, TXT_DOGO = 9, TXT_FLASH = 10;

    /** Plays an animation on an entity for everyone tracking it (and itself if a player). */
    public static void anim(Entity e, MoveType move, int variant) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e),
                new MoveAnimPacket(e.getId(), move, variant));
    }

    /** Camera impact for everyone within {@code radius}; strength falls off with distance on the client. */
    public static void impact(ServerLevel level, Vec3 pos, float trauma, float fov, float radius) {
        impact(level, pos, trauma, fov, radius, ImpactPacket.FLASH_NONE, TXT_NONE, 0);
    }

    public static void impact(ServerLevel level, Vec3 pos, float trauma, float fov, float radius,
                              byte flash, byte text, int lineTicks) {
        MayhemNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        pos.x, pos.y, pos.z, radius + 8.0, level.dimension())),
                new ImpactPacket(pos.x, pos.y, pos.z, trauma, fov, radius, flash, text, (byte) lineTicks));
    }

    /** Impact feedback sent to one player only (e.g. screen flash for the attacker). */
    public static void impactTo(ServerPlayer p, Vec3 pos, float trauma, float fov,
                                byte flash, byte text, int lineTicks) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new ImpactPacket(pos.x, pos.y, pos.z, trauma, fov, 64F, flash, text, (byte) lineTicks));
    }

    public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void particle(ServerLevel level, ParticleOptions type, Vec3 pos, int count,
                                double spread, double speed) {
        level.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    /** Expanding horizontal ring of particles. */
    public static void ring(ServerLevel level, ParticleOptions type, Vec3 center, double startRadius,
                            int points, double speed, double rise) {
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points;
            double dx = Math.cos(a), dz = Math.sin(a);
            // count == 0 makes the (dx, dy, dz) arguments the particle velocity direction
            level.sendParticles(type, center.x + dx * startRadius, center.y, center.z + dz * startRadius,
                    0, dx, rise, dz, speed);
        }
    }

    public static void blockDust(ServerLevel level, BlockState state, Vec3 pos, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.x, pos.y, pos.z, count, 0.35, 0.35, 0.35, 0.12);
    }

    /** Streak of particles from a to b (used for flash-step afterimage trails). */
    public static void trail(ServerLevel level, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        int steps = (int) Math.min(24, Math.max(3, len * 2.0));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.add(d.scale(i / (double) steps)).add(0, 0.9, 0);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.1, 0.35, 0.1, 0.01);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.12, 0.3, 0.12, 0.02);
            }
        }
    }
}
