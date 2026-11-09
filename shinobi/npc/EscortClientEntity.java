package com.mayhem.shinobi.npc;

import com.mayhem.shinobi.story.MissionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Dedicated escort client: follows the assigned player, panics when hurt,
 * and reports proximity/survival to MissionTracker for ESCORT / PROTECT missions.
 */
public class EscortClientEntity extends PathfinderMob {
    private UUID ownerId;
    private int safeTicks;
    private int loseTicks;
    private boolean failed;

    public EscortClientEntity(EntityType<? extends EscortClientEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomNameVisible(true);
        setCustomName(Component.literal("Escort Client"));
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    public void setOwner(ServerPlayer p) {
        this.ownerId = p.getUUID();
        setCustomName(Component.literal("Escort · " + p.getGameProfile().getName()));
    }

    public UUID getOwnerId() { return ownerId; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || failed) return;
        if (!(level() instanceof ServerLevel sl)) return;

        ServerPlayer owner = ownerId == null ? null : sl.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive()) {
            // Wait a bit for relog, then despawn
            if (++loseTicks > 200) discard();
            return;
        }
        loseTicks = 0;

        double dist = distanceTo(owner);
        // Follow owner
        if (dist > 3.5 && dist < 28) {
            getNavigation().moveTo(owner, 1.15D);
        } else if (dist >= 28) {
            // Soft teleport catch-up so mission doesn't soft-lock
            Vec3 behind = owner.position().subtract(owner.getLookAngle().scale(1.5));
            teleportTo(behind.x, owner.getY(), behind.z);
        }

        boolean close = dist <= 10.0;
        if (close) {
            safeTicks++;
            if (safeTicks % 20 == 0) {
                MissionTracker.onEscortTick(owner, true);
            }
        } else if (safeTicks > 0 && tickCount % 40 == 0) {
            owner.displayClientMessage(Component.literal("§eStay closer to your escort client!"), true);
        }

        // Protect: owner gets kill credit only while client is nearby
        // (MissionTracker.onProtectKill is invoked from onKill when hostiles die)
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (!level().isClientSide && ownerId != null && level() instanceof ServerLevel sl) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) {
                owner.displayClientMessage(Component.literal("§cYour escort client is under attack!"), true);
            }
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        failed = true;
        if (!level().isClientSide && ownerId != null && level() instanceof ServerLevel sl) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) {
                owner.displayClientMessage(Component.literal("§cEscort failed — the client was defeated."), false);
                MissionTracker.failEscort(owner);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("owner", ownerId);
        tag.putInt("safeTicks", safeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("owner")) ownerId = tag.getUUID("owner");
        safeTicks = tag.getInt("safeTicks");
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
