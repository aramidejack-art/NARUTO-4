package com.mayhem.progression;

import com.mayhem.config.MayhemConfig;
import com.mayhem.network.MayhemNetwork;
import com.mayhem.network.ProgressionSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * Persistent per-player progression. Everything is stored under one compound
 * ({@link #ROOT_KEY}) inside the player's Forge persistent data, so it is saved
 * with the player and can be copied wholesale when the player entity is cloned
 * (death / returning from the End).
 */
public final class PlayerProgression {
    public static final String ROOT_KEY = "mayhem";
    public static final int MAX_LEVEL = 100;
    public static final int MAX_MASTERY = 30;

    private static final String XP = "xp";
    private static final String LEVEL = "level";
    private static final String STRENGTH = "strength";
    private static final String MASTERY = "mastery";
    private static final String FINISHERS = "finishers";

    private PlayerProgression() {}

    /** Returns (creating if needed) the MAYHEM compound attached to the player. */
    public static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            root.put(ROOT_KEY, new CompoundTag());
        }
        return root.getCompound(ROOT_KEY);
    }

    /** Copies progression from an old player entity to a new one (death, End exit). */
    public static void copy(Player from, Player to) {
        CompoundTag old = from.getPersistentData();
        if (old.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            to.getPersistentData().put(ROOT_KEY, old.getCompound(ROOT_KEY).copy());
        }
    }

    public static int getLevel(Player player) {
        return Math.max(1, data(player).getInt(LEVEL));
    }

    public static int getXp(Player player) {
        return data(player).getInt(XP);
    }

    public static int getStrength(Player player) {
        return data(player).getInt(STRENGTH);
    }

    public static int getMastery(Player player) {
        return data(player).getInt(MASTERY);
    }

    public static float getStrengthBonus(Player player) {
        return Math.min(8.0F, getStrength(player) * 0.35F);
    }

    public static double getReachBonus(Player player) {
        return Math.min(0.45, getMastery(player) * 0.015);
    }

    public static int xpToNext(int level) {
        return 35 + level * 15;
    }

    /** XP for landing a punch on a mob. */
    public static void awardCombat(ServerPlayer player, int amount) {
        addXp(player, Math.max(1, amount));
    }

    /** XP for defeating a mob; scales with the mob's level. */
    public static void awardDefeat(ServerPlayer player, LivingEntity target) {
        addXp(player, 8 + MobLevelHandler.getLevel(target) * 3);
    }

    /**
     * Records a landed finisher hit. Every {@code finishersPerMastery} finishers
     * grants +1 mastery (up to {@link #MAX_MASTERY}).
     */
    public static void recordFinisher(ServerPlayer player) {
        CompoundTag d = data(player);
        int finishers = d.getInt(FINISHERS) + 1;
        int perMastery = MayhemConfig.FINISHERS_PER_MASTERY.get();
        if (finishers >= perMastery) {
            finishers = 0;
            int mastery = d.getInt(MASTERY);
            if (mastery < MAX_MASTERY) {
                d.putInt(MASTERY, mastery + 1);
                player.displayClientMessage(Component.translatable("message.mayhem.mastery_up",
                        mastery + 1), true);
            }
        }
        d.putInt(FINISHERS, finishers);
        sync(player);
    }

    private static void addXp(ServerPlayer player, int amount) {
        CompoundTag d = data(player);
        int level = getLevel(player);
        int xp = d.getInt(XP) + amount;
        int gained = 0;

        while (level < MAX_LEVEL && xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
            d.putInt(STRENGTH, d.getInt(STRENGTH) + 1);
            gained++;
        }
        if (level >= MAX_LEVEL) {
            xp = 0;
        }

        d.putInt(XP, xp);
        d.putInt(LEVEL, level);

        if (gained > 0) {
            player.displayClientMessage(Component.translatable("message.mayhem.level_up", level), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.7F, 1.0F);
        }
        sync(player);
    }

    /** Prints the full status line to the player's chat. */
    public static void sendStatus(ServerPlayer player) {
        int level = getLevel(player);
        String xp = level >= MAX_LEVEL ? "MAX" : getXp(player) + "/" + xpToNext(level);
        player.displayClientMessage(Component.translatable("message.mayhem.status",
                level, xp, getStrength(player), getMastery(player)), false);
    }

    /** Pushes the current progression to the owning client (for the HUD). */
    public static void sync(ServerPlayer player) {
        int level = getLevel(player);
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ProgressionSyncPacket(level, getXp(player),
                        level >= MAX_LEVEL ? 0 : xpToNext(level),
                        getStrength(player), getMastery(player)));
    }
}
