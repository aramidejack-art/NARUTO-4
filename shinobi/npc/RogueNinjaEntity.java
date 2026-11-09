package com.mayhem.shinobi.npc;

import com.mayhem.shinobi.combat.EnemyCombat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Hostile ninja using EnemyCombat. Higher jinchūriki roll chance. */
public class RogueNinjaEntity extends PathfinderMob implements Enemy {
    private static final String[] NAMES = {
            "Rogue Genin", "Missing-nin", "Clan Hunter", "Night Blade", "Ash Operative", "Road Bandit"
    };

    public RogueNinjaEntity(EntityType<? extends RogueNinjaEntity> type, Level level) {
        super(type, level);
        addTag(EnemyCombat.TAG_ROGUE);
        getPersistentData().putBoolean(EnemyCombat.TAG_ROGUE, true);
        setPersistenceRequired();
        if (!level.isClientSide) {
            setCustomName(Component.literal(NAMES[getRandom().nextInt(NAMES.length)]));
            setCustomNameVisible(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ARMOR, 3.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(EnemyCombat.TAG_ROGUE, true);
        if (getPersistentData().contains(EnemyCombat.NBT_BEAST)) {
            tag.putString(EnemyCombat.NBT_BEAST, getPersistentData().getString(EnemyCombat.NBT_BEAST));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        getPersistentData().putBoolean(EnemyCombat.TAG_ROGUE, true);
        addTag(EnemyCombat.TAG_ROGUE);
        if (tag.contains(EnemyCombat.NBT_BEAST)) {
            getPersistentData().putString(EnemyCombat.NBT_BEAST, tag.getString(EnemyCombat.NBT_BEAST));
            addTag(EnemyCombat.TAG_JIN);
        }
    }
}
