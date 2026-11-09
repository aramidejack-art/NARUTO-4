package com.mayhem.shinobi.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A village shinobi with a role. Talking to it opens a dialogue screen; all choices are validated on the server. */
public class NpcEntity extends PathfinderMob {
    public enum Role {
        SENSEI("Sensei", "story"), MISSION_GIVER("Mission Clerk", "contracts"), TRAINER("Trainer", "training"), MERCHANT("Merchant", "shop");
        public final String title, tag;
        Role(String title, String tag) { this.title = title; this.tag = tag; }
    }

    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);
    private static final String[] NAMES = {"Haruto", "Mizuki", "Daigo", "Rin", "Kaito", "Sora", "Akane", "Ren"};

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        addTag("mayhem_friendly");
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(ROLE, 0); }

    public Role role() { return Role.values()[Math.max(0, Math.min(Role.values().length - 1, entityData.get(ROLE)))]; }

    public void setRole(Role r) {
        entityData.set(ROLE, r.ordinal());
        setCustomName(Component.literal(NAMES[Math.floorMod(getUUID().hashCode(), NAMES.length)] + " the " + r.title));
        setCustomNameVisible(true);
    }

    public String displayName() { return getCustomName() == null ? role().title : getCustomName().getString(); }

    @Override public boolean hurt(DamageSource src, float amount) {
        return src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(src, amount);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) Dialogues.open(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("npcRole", entityData.get(ROLE)); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t);
        entityData.set(ROLE, Math.max(0, Math.min(Role.values().length - 1, t.getInt("npcRole"))));
        if (getCustomName() == null) setRole(role()); // structure-placed NPCs get their name on first load
    }
}
