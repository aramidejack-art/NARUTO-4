package com.mayhem.shinobi.fx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Visual-only body of a flying jutsu: the server projectile moves it, the client draws a 3D shape for its element. */
public class JutsuFxEntity extends Entity {
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(JutsuFxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(JutsuFxEntity.class, EntityDataSerializers.FLOAT);

    public JutsuFxEntity(EntityType<? extends JutsuFxEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override protected void defineSynchedData() {
        entityData.define(STYLE, 0);
        entityData.define(SIZE, 0.6F);
    }

    public int style() { return entityData.get(STYLE); }
    public float size() { return entityData.get(SIZE); }
    public void setLook(int style, float size) { entityData.set(STYLE, style); entityData.set(SIZE, size); }

    /** Faces along the travel direction. */
    public void aim(double vx, double vy, double vz) {
        double h = Math.sqrt(vx * vx + vz * vz);
        setYRot((float) (Math.toDegrees(Math.atan2(-vx, vz))));
        setXRot((float) (-Math.toDegrees(Math.atan2(vy, h))));
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
