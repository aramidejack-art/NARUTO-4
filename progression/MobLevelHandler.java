package com.mayhem.progression;

import com.mayhem.config.MayhemConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public final class MobLevelHandler {
    public static final String LEVEL_KEY = "mayhem_mob_level";
    private static final String INITIALIZED_KEY = "mayhem_level_initialized";

    // Fixed UUIDs so the permanent modifiers are saved with the mob and never duplicated.
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("5d0f1c3a-7a2e-4c55-9a6b-2a8f4b7e1001");
    private static final UUID DAMAGE_MODIFIER_ID = UUID.fromString("5d0f1c3a-7a2e-4c55-9a6b-2a8f4b7e1002");

    /** Level of a (possibly unlevelled) entity; always at least 1. */
    public static int getLevel(Entity entity) {
        return Math.max(1, entity.getPersistentData().getInt(LEVEL_KEY));
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!MayhemConfig.MOB_LEVELS_ENABLED.get()) return;
        if (mob.getPersistentData().getBoolean(INITIALIZED_KEY)) return;
        if (!shouldLevel(mob)) return;

        int min = Math.min(MayhemConfig.MOB_MIN_LEVEL.get(), MayhemConfig.MOB_MAX_LEVEL.get());
        int max = Math.max(MayhemConfig.MOB_MIN_LEVEL.get(), MayhemConfig.MOB_MAX_LEVEL.get());
        int level = min + mob.getRandom().nextInt(max - min + 1);

        mob.getPersistentData().putInt(LEVEL_KEY, level);
        mob.getPersistentData().putBoolean(INITIALIZED_KEY, true);

        int steps = level - 1;
        if (steps <= 0) return;

        // Modifiers (rather than overwriting base values) stay compatible with mobs and
        // other mods that adjust attributes in finalizeSpawn or at runtime.
        boolean healthChanged = applyModifier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID,
                "MAYHEM level health", steps * MayhemConfig.HEALTH_PER_LEVEL.get());
        applyModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID,
                "MAYHEM level damage", steps * MayhemConfig.DAMAGE_PER_LEVEL.get());

        if (healthChanged) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private static boolean shouldLevel(Mob mob) {
        if (mob.getType().is(Tags.EntityTypes.BOSSES)) {
            return MayhemConfig.LEVEL_BOSSES.get();
        }
        return mob instanceof Enemy || MayhemConfig.LEVEL_PASSIVE_MOBS.get();
    }

    private static boolean applyModifier(AttributeInstance instance, UUID id, String name, double amount) {
        if (instance == null || amount <= 0 || instance.getModifier(id) != null) return false;
        instance.addPermanentModifier(new AttributeModifier(id, name, amount,
                AttributeModifier.Operation.MULTIPLY_BASE));
        return true;
    }
}
