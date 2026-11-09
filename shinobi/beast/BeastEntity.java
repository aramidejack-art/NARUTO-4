package com.mayhem.shinobi.beast;

import com.mayhem.fx.Fx;
import com.mayhem.fx.MayhemSounds;
import com.mayhem.shinobi.beast.Beasts.Atk;
import com.mayhem.shinobi.beast.Beasts.Beast;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.story.MissionTracker;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

/**
 * Tailed-beast encounter boss. Three damage phases with telegraphed attacks (slam, charge, breath, roar, bomb).
 * It cannot be killed by damage: at 12% health it kneels (weakened) and can be sealed with a Sealing Scroll.
 */
public class BeastEntity extends Monster {
    private static final EntityDataAccessor<String> BEAST_ID = SynchedEntityData.defineId(BeastEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> WINDUP = SynchedEntityData.defineId(BeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WEAKENED = SynchedEntityData.defineId(BeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Random RNG = new Random();

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Tailed Beast"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int attackCd = 80, telegraph, weakTicks, active, channel;
    private Atk pending;
    private Vec3 aim = Vec3.ZERO;

    public BeastEntity(EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
        this.xpReward = 300;
        this.setPersistenceRequired();
        this.addTag("mayhem_beast");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400).add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 6);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BEAST_ID, "sand_tanuki");
        entityData.define(WEAKENED, false);
        entityData.define(WINDUP, false);
    }

    public String beastId() { return entityData.get(BEAST_ID); }
    public boolean isWeakened() { return entityData.get(WEAKENED); }
    /** Client-visible wind-up flag for the model (set from the synced telegraph counter via tickCount parity is not reliable, so it is synced). */
    public boolean isTelegraphing() { return entityData.get(WINDUP); }
    public Beast kit() { Beast b = Beasts.get(beastId()); return b == null ? Beasts.get("sand_tanuki") : b; }

    public void setBeast(String id) {
        Beast b = Beasts.get(id);
        if (b == null) return;
        entityData.set(BEAST_ID, id);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(b.health() * 4.0);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(b.damage());
        setHealth(getMaxHealth());
        setCustomName(Component.literal(b.name()));
        bar.setName(Component.literal(b.name()));
    }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putString("beastId", beastId()); t.putBoolean("weak", isWeakened()); }
    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("beastId")) setBeast(t.getString("beastId"));
        entityData.set(WEAKENED, t.getBoolean("weak"));
        if (isWeakened()) { setNoAi(true); weakTicks = 600; }
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canChangeDimensions() { return false; }

    @Override public boolean hurt(DamageSource src, float amount) {
        if (isWeakened()) return false;
        if (src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(src, amount);
        float floor = getMaxHealth() * 0.12F;
        if (getHealth() - amount <= floor) {
            setHealth(floor);
            kneel();
            return false;
        }
        return super.hurt(src, amount);
    }

    private void kneel() {
        entityData.set(WEAKENED, true);
        setNoAi(true);
        weakTicks = 1200;
        telegraph = 0;
        if (level() instanceof ServerLevel sl) {
            Fx.sound(sl, position(), MayhemSounds.BOOM.get(), 2F, 0.5F);
            Fx.impact(sl, position(), 1.0F, 0.05F, 40F);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) {
                MissionTracker.onBeast(p, beastId(), false);
                CompoundTag t = ShinobiData.of(p);
                if (ShinobiData.add(t, "beastsDefeated", beastId())) t.putInt("ap", t.getInt("ap") + 3);
                ShinobiNet.banner(p, 2, kit().name() + " kneels", "Use a Sealing Scroll on it within 60 seconds");
                ShinobiNet.syncTag(p);
            }
        }
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (isWeakened() && tickCount % 6 == 0) level().addParticle(ParticleTypes.SOUL, getRandomX(0.8), getY() + 1, getRandomZ(0.8), 0, 0.05, 0);
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        entityData.set(WINDUP, telegraph > 0);
        bar.setProgress(Math.max(0F, getHealth() / getMaxHealth()));
        bar.setColor(isWeakened() ? BossEvent.BossBarColor.WHITE : phase() == 2 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
        if (isWeakened()) {
            if (--weakTicks <= 0) {
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) ShinobiNet.banner(p, 0, kit().name() + " escaped", "It vanished into the wild");
                sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.5, getZ(), 60, 1.5, 1.5, 1.5, 0.1);
                discard();
            }
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) { telegraph = 0; channel = 0; return; }

        if (channel > 0) { channel(sl, target); return; }
        if (telegraph > 0) {
            telegraph--;
            telegraphFx(sl);
            if (telegraph == 0) execute(sl, target);
            return;
        }
        if (--attackCd > 0) return;
        double dist = distanceTo(target);
        Atk[] pool = kit().phases()[phase()];
        Atk pick = pool[RNG.nextInt(pool.length)];
        if (pick == Atk.CHARGE && dist < 6) pick = Atk.SLAM;
        if (pick == Atk.SLAM && dist > 12) pick = Atk.CHARGE;
        if (pick == Atk.BREATH && dist > 20) pick = Atk.CHARGE;
        pending = pick;
        aim = target.position();
        telegraph = switch (pick) { case SLAM -> 24; case CHARGE -> 20; case BREATH -> 18; case ROAR -> 26; case BOMB -> 70; };
        Fx.sound(sl, position(), pick == Atk.BOMB ? MayhemSounds.CHARGE.get() : SoundEvents.WARDEN_ROAR, 1.6F, pick == Atk.BOMB ? 0.5F : 0.8F);
    }

    private int phase() {
        float f = getHealth() / getMaxHealth();
        return f > 0.66F ? 0 : f > 0.33F ? 1 : 2;
    }

    private DustParticleOptions dust() {
        int c = kit().color();
        return new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), 2.2F);
    }

    private void telegraphFx(ServerLevel sl) {
        if (pending == null) return;
        double r = switch (pending) { case SLAM -> 7.5; case ROAR -> 12; case BOMB -> 12; default -> 2.0; };
        Vec3 c = (pending == Atk.BOMB) ? aim : position();
        if (pending == Atk.CHARGE || pending == Atk.BREATH) {
            Vec3 d = aim.subtract(position()).multiply(1, 0, 1).normalize();
            for (double k = 2; k < 16; k += 1.0) sl.sendParticles(dust(), getX() + d.x * k, getY() + 0.2, getZ() + d.z * k, 1, 0.15, 0, 0.15, 0);
        } else {
            Fx.ring(sl, dust(), c.add(0, 0.15, 0), r, (int) (r * 9), 0, 0);
        }
    }

    private void hit(ServerLevel sl, LivingEntity e, double mult, double knock, double lift, Vec3 from) {
        if (e == this || e instanceof BeastEntity || e.getTags().contains("mayhem_friendly") && false) return;
        e.invulnerableTime = 0;
        e.hurt(damageSources().mobAttack(this), (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult));
        Vec3 d = e.position().subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() > 1.0E-4) d = d.normalize();
        e.setDeltaMovement(e.getDeltaMovement().add(d.scale(knock)).add(0, lift, 0));
        e.hurtMarked = true;
    }

    private void execute(ServerLevel sl, LivingEntity target) {
        Atk a = pending;
        pending = null;
        attackCd = 50 - phase() * 10 + RNG.nextInt(30);
        Vec3 me = position();
        switch (a) {
            case SLAM -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7.5, 3, 7.5), x -> x != this)) hit(sl, e, 1.0, 1.5, 0.7, me);
                Fx.ring(sl, ParticleTypes.POOF, me.add(0, 0.2, 0), 1, 40, 0.5, 0.05);
                Fx.sound(sl, me, MayhemSounds.BOOM.get(), 2F, 0.7F);
                Fx.impact(sl, me, 0.8F, 0.04F, 36F);
            }
            case CHARGE -> {
                Vec3 d = aim.subtract(me).multiply(1, 0, 1).normalize();
                setDeltaMovement(d.x * 2.2, 0.25, d.z * 2.2);
                hurtMarked = true;
                active = 14;
                aim = d;
                channel = 14;
                Fx.sound(sl, me, SoundEvents.RAVAGER_ROAR, 2F, 0.6F);
            }
            case BREATH -> {
                aim = aim.subtract(me).multiply(1, 0, 1).normalize();
                channel = 30;
                active = 30;
            }
            case ROAR -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12), x -> x != this)) {
                    hit(sl, e, 0.4, 1.8, 0.5, me);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                Fx.ring(sl, ParticleTypes.CLOUD, me.add(0, 1, 0), 1, 40, 0.8, 0.02);
                Fx.sound(sl, me, SoundEvents.WARDEN_SONIC_BOOM, 2F, 0.6F);
                Fx.impact(sl, me, 0.9F, 0.05F, 40F);
            }
            case BOMB -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(aim, aim).inflate(12, 6, 12), x -> x != this)) hit(sl, e, 2.5, 2.2, 1.0, aim);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, aim.x, aim.y + 1, aim.z, 3, 2, 1, 2, 0);
                Fx.ring(sl, dust(), aim.add(0, 0.3, 0), 1, 50, 1.0, 0.05);
                Fx.sound(sl, aim, MayhemSounds.BOOM.get(), 3F, 0.5F);
                Fx.impact(sl, aim, 1.2F, 0.08F, 60F, (byte) 1, (byte) 5, 6);
            }
        }
    }

    private void channel(ServerLevel sl, LivingEntity target) {
        channel--;
        Vec3 me = position();
        if (pendingChannelIsCharge()) {
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.8), x -> x != this)) hit(sl, e, 1.2, 1.4, 0.5, me);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 8, 1, 0.2, 1, 0.05);
        } else {
            for (double k = 2; k < 14; k += 1.2) {
                Vec3 pt = me.add(aim.x * k, 1.2, aim.z * k);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 3, 0.3 + k * 0.03, 0.3, 0.3 + k * 0.03, 0.02);
                sl.sendParticles(dust(), pt.x, pt.y, pt.z, 2, 0.3, 0.3, 0.3, 0);
            }
            if (channel % 5 == 0) {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(14, 4, 14), x -> x != this)) {
                    Vec3 d = e.position().subtract(me).multiply(1, 0, 1);
                    double len = d.length();
                    if (len < 14 && len > 0.1 && d.normalize().dot(aim) > 0.85) { hit(sl, e, 0.55, 0.5, 0.15, me); e.setSecondsOnFire(3); }
                }
            }
        }
    }

    private boolean pendingChannelIsCharge() { return active == 14 || (active > 0 && active < 15 && getDeltaMovement().horizontalDistanceSqr() > 0.2); }

    @Override public void die(DamageSource src) { bar.removeAllPlayers(); super.die(src); }
    @Override public void remove(RemovalReason r) { bar.removeAllPlayers(); super.remove(r); }

    /** Players that may take part in an encounter: used for banner messaging. */
    public List<ServerPlayer> participants() {
        return level() instanceof ServerLevel sl ? sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48)) : List.of();
    }
}
