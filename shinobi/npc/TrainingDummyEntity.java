package com.mayhem.shinobi.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Immobile training dummy for practicing taijutsu and jutsu.
 * Does not fight back, regenerates HP, shows recent damage as floating name.
 */
public class TrainingDummyEntity extends Mob {
    private float lastHit;
    private int lastHitTicks;
    private float totalDamage;

    public TrainingDummyEntity(EntityType<? extends TrainingDummyEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setCustomNameVisible(true);
        setCustomName(Component.literal("Training Dummy"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isNoGravity() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (amount <= 0) return false;
        lastHit = amount;
        lastHitTicks = 40;
        totalDamage += amount;
        boolean ok = super.hurt(source, amount);
        setCustomName(Component.literal(String.format("Dummy · last %.1f · total %.0f", lastHit, totalDamage)));
        // Soft-reset instead of dying so training continues
        if (getHealth() <= 0.5F) {
            setHealth(getMaxHealth());
            totalDamage = 0;
            setCustomName(Component.literal("Training Dummy · reset"));
        }
        return ok;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (lastHitTicks > 0) {
            lastHitTicks--;
            if (lastHitTicks == 0) setCustomName(Component.literal(String.format("Dummy · total %.0f", totalDamage)));
        }
        // Slow regen for prolonged training
        if (tickCount % 40 == 0 && getHealth() < getMaxHealth()) {
            heal(2.0F);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("totalDamage", totalDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        totalDamage = tag.getFloat("totalDamage");
    }
}
