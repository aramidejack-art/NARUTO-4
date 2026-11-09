package com.mayhem.shinobi.combat;

import com.mayhem.shinobi.beast.Beasts;
import com.mayhem.shinobi.data.ShinobiData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Rare enemy jinchūriki rolls; skips beasts already held by online players. */
public final class EnemyJinchuriki {
    public static final double CHANCE_NORMAL = 0.03;
    public static final double CHANCE_ROGUE = 0.08;

    private EnemyJinchuriki() {}

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (!(e.getEntity() instanceof Mob mob)) return;
        boolean rogue = mob.getPersistentData().getBoolean(EnemyCombat.TAG_ROGUE) || mob.getTags().contains(EnemyCombat.TAG_ROGUE);
        if (!(mob instanceof Enemy) && !rogue) return;
        if (mob.getPersistentData().contains(EnemyCombat.NBT_BEAST)) return;
        if (mob.getPersistentData().getBoolean("mayhemJinRolled")) return;
        mob.getPersistentData().putBoolean("mayhemJinRolled", true);

        double chance = rogue ? CHANCE_ROGUE : CHANCE_NORMAL;
        if (ThreadLocalRandom.current().nextDouble() > chance) return;

        Set<String> taken = takenBeasts(mob);
        List<String> pool = new ArrayList<>();
        for (String id : Beasts.ALL.keySet()) if (!taken.contains(id)) pool.add(id);
        if (pool.isEmpty()) return;

        apply(mob, pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }

    public static Set<String> takenBeasts(Mob context) {
        Set<String> taken = new HashSet<>();
        if (!(context.level() instanceof net.minecraft.server.level.ServerLevel sl)) return taken;
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            String id = ShinobiData.beast(ShinobiData.of(p));
            if (id != null && !id.isEmpty()) taken.add(id);
        }
        return taken;
    }

    public static void apply(Mob mob, String beastId) {
        Beasts.Beast b = Beasts.get(beastId);
        if (b == null) return;
        mob.getPersistentData().putString(EnemyCombat.NBT_BEAST, beastId);
        mob.addTag(EnemyCombat.TAG_JIN);
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * 1.5);
            mob.setHealth(mob.getMaxHealth());
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * 1.2);
        }
        String name = mob.hasCustomName() ? mob.getCustomName().getString() : mob.getName().getString();
        mob.setCustomName(Component.literal("§6Jinchūriki §f" + name + " §7(" + b.name() + ")"));
        mob.setCustomNameVisible(true);
    }
}
