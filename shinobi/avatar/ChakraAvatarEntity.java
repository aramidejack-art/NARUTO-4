package com.mayhem.shinobi.avatar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/**
 * The visible chakra body of a jinchuriki (hollow tailed-beast avatar) or of the Mirror Eye Guardian (Susanoo).
 * It is purely a body: it follows its owner every tick, so the owner keeps vanilla movement (smooth, no vehicle lag),
 * and it only carries synced pose data (stance, current action, stage) for the renderer.
 */
public class ChakraAvatarEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ChakraAvatarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> FORM = SynchedEntityData.defineId(ChakraAvatarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> STAND = SynchedEntityData.defineId(ChakraAvatarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACT = SynchedEntityData.defineId(ChakraAvatarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACT_START = SynchedEntityData.defineId(ChakraAvatarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(ChakraAvatarEntity.class, EntityDataSerializers.INT);

    /** Duration in ticks of each action animation (index = act id). */
    public static final int[] ACT_LEN = {1, 8, 8, 14, 18};

    /** Client-side eased stance (0 = all fours, 1 = standing). */
    public float standAmt, standAmtO;

    public ChakraAvatarEntity(EntityType<? extends ChakraAvatarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER, Optional.empty());
        entityData.define(FORM, "");
        entityData.define(STAND, false);
        entityData.define(ACT, 0);
        entityData.define(ACT_START, 0);
        entityData.define(STAGE, 1);
    }

    public UUID ownerId() { return entityData.get(OWNER).orElse(null); }
    public void setOwner(UUID id) { entityData.set(OWNER, Optional.ofNullable(id)); }
    public String form() { return entityData.get(FORM); }
    public void setForm(String f) { entityData.set(FORM, f); }
    public boolean standing() { return entityData.get(STAND); }
    public void setStanding(boolean b) { entityData.set(STAND, b); }
    public int act() { return entityData.get(ACT); }
    public int stage() { return entityData.get(STAGE); }
    public void setStage(int s) { entityData.set(STAGE, s); }
    public boolean isMode() { return form().startsWith("mode:"); }
    public boolean isGuardian() { return "guardian".equals(form()); }

    public void startAct(int act) {
        entityData.set(ACT, act);
        entityData.set(ACT_START, (int) (level().getGameTime() & 0x7FFFFFFF));
    }

    /** 0..1 progress of the current action, or 1 when finished / none. */
    public float actProgress(float partial) {
        int a = act();
        if (a <= 0 || a >= ACT_LEN.length) return 1F;
        float t = (float) (level().getGameTime() & 0x7FFFFFFF) - entityData.get(ACT_START) + partial;
        return Math.max(0F, t / ACT_LEN[a]);
    }

    public Player owner() {
        UUID id = ownerId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    @Override
    public void tick() {
        super.tick();
        Player o = owner();
        if (!level().isClientSide) {
            if (o == null || !o.isAlive() || !AvatarManager.wanted(o, form()).equals(form())) { discard(); return; }
            if (act() > 0 && actProgress(0F) >= 1F) entityData.set(ACT, 0);
        }
        if (o != null) {
            setPos(o.getX(), o.getY(), o.getZ());
            setYRot(o.yBodyRot);
        }
        standAmtO = standAmt;
        standAmt += ((standing() ? 1F : 0F) - standAmt) * 0.2F;
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
