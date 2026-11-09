package com.mayhem.shinobi.npc;

import com.mayhem.combat.MoveType;
import com.mayhem.progression.MobLevelHandler;
import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.combat.EnemyCombat;
import com.mayhem.shinobi.net.ShinobiNet;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A strong shinobi you stumble across while travelling. Every one has a rank (genin to legend) that decides its level, health,
 * armour, gear, skin and special techniques, and a mood: hostile (attacks on sight), duelist (waits to be challenged) or
 * friendly (talks, shares rumours, will spar). Duels end when the loser drops to 25% health: the NPC yields and rewards you
 * instead of dying. Everything is decided on the server; the client only needs the rank (for the skin).
 */
public class WandererEntity extends PathfinderMob {
    public enum Mood { HOSTILE, DUELIST, FRIENDLY }

    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(WandererEntity.class, EntityDataSerializers.INT);
    private static final String FRIENDLY_TAG = "mayhem_friendly";
    private static final String[] FIRST = {"Kaito", "Mizuki", "Daigo", "Rin", "Haruto", "Sora", "Akane", "Ren", "Takeshi", "Hana", "Ryuu", "Satsuki", "Jiro", "Yuna", "Gen", "Nagi"};
    private static final String[] EPITHET = {"of the Ash Road", "of the Hollow Pass", "the Quiet Blade", "of the Red Dunes", "the Wandering", "of the Black Pines", "the Storm-Walker", "of the Salt Coast"};
    private static final String[][] GREET = {
            {"Hey, you look like you can fight! Care to trade a few blows?"},
            {"Another shinobi on the road. Let's see how you measure up."},
            {"Your chakra is interesting. Show me what you have learned."},
            {"Few people meet my eyes for long. Prove you belong on this road."},
            {"So you have come. I have waited a long time for someone worth fighting."}};
    private static final String[] FRIENDLY_LINE = {
            "Safe travels! The roads are rougher than they look.",
            "Good fight. The road is long, keep sharpening yourself.",
            "You fight well. I am in no hurry, ask what you like.",
            "Strength is only the beginning. Ask, and I may answer.",
            "You have earned a few words from me. Use them well."};
    private static final String[] BEGIN = {"Here I come!", "Don't hold back!", "Come.", "Show me.", "Do not disappoint me."};
    private static final String[] YIELD = {
            "Ow, ow... alright, you win!", "I yield. You're stronger than you look.", "Enough. You have earned my respect.",
            "...Impressive. I yield.", "Remarkable. No one has pushed me this far in a very long time."};

    private Mood mood = Mood.DUELIST;
    private boolean duel, yielded, noticed, configured;
    private int windup, jutsuCd = 80, flickerCd = 120;
    private ServerBossEvent bar;
    private final Set<UUID> beaten = new HashSet<>();

    public WandererEntity(EntityType<? extends WandererEntity> type, Level level) {
        super(type, level);
        addTag(EnemyCombat.TAG_ROGUE);          // lets the telegraphed-strike combat brain drive its melee
        getPersistentData().putBoolean(EnemyCombat.TAG_ROGUE, true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(RANK, 0); }

    // ------------------------------------------------------------------------------------------------ setup

    public Rank rank() { return Rank.byIndex(entityData.get(RANK)); }
    public Mood mood() { return mood; }
    public boolean dueling() { return duel; }
    /** True while this shinobi will not fight unless provoked (so area attacks and avatars leave it alone). */
    public boolean isPeaceful() { return mood == Mood.FRIENDLY || (mood == Mood.DUELIST && !duel); }
    public String displayName() { return getCustomName() == null ? rank().title : getCustomName().getString(); }

    /** Rolls stats, gear, mood and name for a given rank, scaled to the player's level. */
    public void setup(Rank r, int playerLevel) {
        configured = true;
        entityData.set(RANK, r.ordinal());
        int lvl = Mth.clamp(playerLevel + r.levelOffset + random.nextInt(5) - 2, 1, 100);
        getPersistentData().putInt(MobLevelHandler.LEVEL_KEY, lvl);
        getPersistentData().putBoolean("mayhem_level_initialized", true);   // the generic mob-level handler must not rescale us
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0 + lvl * r.hpPerLevel);
        setHealth(getMaxHealth());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.0 + lvl * 0.04 * r.powerMult);
        getAttribute(Attributes.ARMOR).setBaseValue(r.armor);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(r.kbResist);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.26 + r.ordinal() * 0.02);

        int[] hostile = {60, 45, 30, 20, 10};
        int[] duelist = {0, 20, 40, 50, 60};
        int roll = random.nextInt(100);
        int i = r.ordinal();
        mood = roll < hostile[i] ? Mood.HOSTILE : roll < hostile[i] + duelist[i] ? Mood.DUELIST : Mood.FRIENDLY;

        ItemStack weapon = switch (r) {
            case GENIN -> new ItemStack(Items.WOODEN_SWORD);
            case CHUNIN, JONIN -> new ItemStack(Items.IRON_SWORD);
            case ELITE -> new ItemStack(Items.DIAMOND_SWORD);
            default -> new ItemStack(Items.GOLDEN_SWORD);
        };
        setItemSlot(EquipmentSlot.MAINHAND, weapon);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        String nm = FIRST[random.nextInt(FIRST.length)] + " " + EPITHET[random.nextInt(EPITHET.length)];
        setCustomName(Component.empty().append(r.tag()).append(Component.literal(" " + nm).withStyle(ChatFormatting.WHITE)));
        setCustomNameVisible(true);
        if (r.ordinal() >= Rank.ELITE.ordinal()) makeBar();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, diff, reason, data, tag);
        if (!configured) {   // spawn egg or /summon: size it up against the nearest player
            Player np = level.getNearestPlayer(this, 64.0);
            int pl = np == null ? 5 : PlayerProgression.getLevel(np);
            setup(Rank.roll(pl, random), pl);
        }
        return out;
    }

    private void makeBar() {
        bar = new ServerBossEvent(getDisplayName(), rank() == Rank.LEGEND ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS);
    }

    @Override public void setCustomName(Component name) {
        super.setCustomName(name);
        if (bar != null) bar.setName(getDisplayName());
    }

    @Override public void startSeenByPlayer(ServerPlayer sp) { super.startSeenByPlayer(sp); if (bar != null) bar.addPlayer(sp); }
    @Override public void stopSeenByPlayer(ServerPlayer sp) { super.stopSeenByPlayer(sp); if (bar != null) bar.removePlayer(sp); }
    @Override public void remove(Entity.RemovalReason reason) {
        if (bar != null) bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    // ------------------------------------------------------------------------------------------------ talk

    public String greeting() { return GREET[rank().ordinal()][0]; }
    public String friendlyLine() { return FRIENDLY_LINE[rank().ordinal()]; }

    public String introduction() {
        int lvl = MobLevelHandler.getLevel(this);
        return "I am " + displayName().replaceFirst("^\\[[A-Za-z]+\\] ", "") + ", a " + rank().title + " of the wandering shinobi. Strength: about "
                + rank().power(lvl) + ".";
    }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (mood == Mood.HOSTILE && getTarget() != null) return InteractionResult.PASS;
        WandererDialogue.open(sp, this);
        return InteractionResult.CONSUME;
    }

    private void say(ServerPlayer sp, String text) {
        sp.sendSystemMessage(Component.empty().append(getDisplayName()).append(Component.literal(": " + text).withStyle(ChatFormatting.GRAY)));
    }

    // ------------------------------------------------------------------------------------------------ duels

    public void beginDuel(ServerPlayer sp) {
        duel = true; yielded = false;
        if (mood == Mood.FRIENDLY) mood = Mood.DUELIST;
        setTarget(sp);
        say(sp, BEGIN[rank().ordinal()]);
    }

    private void yieldTo(ServerPlayer sp) {
        yielded = true; duel = false; mood = Mood.FRIENDLY;
        setTarget(null);
        getNavigation().stop();
        setHealth(Math.max(getHealth(), getMaxHealth() * 0.25F));
        say(sp, YIELD[rank().ordinal()]);
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 14, 0.5, 0.6, 0.5, 0.02);
        if (!beaten.add(sp.getUUID())) return;      // rewarded once per player
        Rank r = rank();
        int xp = 20 + MobLevelHandler.getLevel(this) * 4;
        PlayerProgression.awardCombat(sp, xp);
        give(sp, new ItemStack(Items.EMERALD, r.emeralds * 2 + random.nextInt(r.emeralds + 1)));
        if (r.ordinal() >= Rank.ELITE.ordinal() && random.nextInt(100) < 40) give(sp, new ItemStack(ShinobiRegistry.SEALING_SCROLL.get()));
        ShinobiNet.banner(sp, 3, "Duel won", r.title + " yields  +" + xp + " XP");
    }

    private static void give(ServerPlayer sp, ItemStack s) {
        if (!sp.getInventory().add(s)) sp.drop(s, false);
    }

    @Override public boolean hurt(DamageSource src, float amount) {
        if (!level().isClientSide && src.getEntity() instanceof ServerPlayer sp) {
            if (yielded || (mood == Mood.FRIENDLY && !duel)) { yielded = false; mood = Mood.HOSTILE; duel = false; setTarget(sp); }   // you struck a friend
            else if (mood == Mood.DUELIST && !duel) { duel = true; setTarget(sp); say(sp, BEGIN[rank().ordinal()]); }
            if (duel && !yielded && getHealth() - amount <= getMaxHealth() * 0.25F) { yieldTo(sp); return false; }
        }
        return super.hurt(src, amount);
    }

    @Override protected void dropCustomDeathLoot(DamageSource src, int looting, boolean recent) {
        super.dropCustomDeathLoot(src, looting, recent);
        Rank r = rank();
        spawnAtLocation(new ItemStack(Items.EMERALD, r.emeralds + random.nextInt(r.emeralds + 1)));
        if (r.ordinal() >= Rank.JONIN.ordinal() && random.nextInt(100) < 15 + r.ordinal() * 10) spawnAtLocation(new ItemStack(ShinobiRegistry.SEALING_SCROLL.get()));
    }

    /** Damage of a telegraphed strike (called by EnemyCombat); gentler than the generic mob curve so a strong NPC stays beatable. */
    public float strikeDamage(MoveType move) {
        float base = 2.5F + MobLevelHandler.getLevel(this) * 0.12F * (0.7F + 0.15F * rank().ordinal());
        if (move.isKick() || move == MoveType.UPPERCUT || move == MoveType.WHIRLWIND) base *= 1.35F;
        if (move == MoveType.JAB) base *= 0.85F;
        return Math.min(base, 18F);
    }

    // ------------------------------------------------------------------------------------------------ server brain

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        Rank r = rank();
        if (bar != null && tickCount % 5 == 0) bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0F, 1F));
        if (tickCount % 10 == 0) scan(sl);
        if (r.ordinal() >= Rank.JONIN.ordinal() && tickCount % 6 == 0) aura(sl, r);
        if (yielded && tickCount % 20 == 0 && getTarget() == null) heal(getMaxHealth() * 0.01F);
        if (tickCount % 200 == 0 && getTarget() == null && (sl.getNearestPlayer(this, 160.0) == null || tickCount > 36000)) { discard(); return; }

        LivingEntity tg = getTarget();
        if (windup > 0) { windup(sl, tg); return; }
        if (jutsuCd > 0) jutsuCd--;
        if (flickerCd > 0) flickerCd--;
        if (tg instanceof ServerPlayer sp && sp.isAlive()) fightExtras(sl, sp, r);
    }

    private void scan(ServerLevel sl) {
        Player np = sl.getNearestPlayer(this, 40.0);
        if (!(np instanceof ServerPlayer sp)) { noticed = false; if (duel) { duel = false; setTarget(null); } return; }
        double d = distanceTo(sp);
        if (d > 34) noticed = false;
        if (!noticed && d < 18 && hasLineOfSight(sp)) { noticed = true; announce(sp); }
        if (mood == Mood.HOSTILE && getTarget() == null && d < 20 && !sp.isCreative() && !sp.isSpectator() && hasLineOfSight(sp)) setTarget(sp);
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || distanceTo(t) > 48)) { setTarget(null); if (duel) duel = false; }
    }

    private void announce(ServerPlayer sp) {
        int mine = MobLevelHandler.getLevel(this);
        int myPow = rank().power(mine), yourPow = Rank.basePower(PlayerProgression.getLevel(sp));
        double ratio = myPow / (double) Math.max(1, yourPow);
        String cmp = ratio >= 2.5 ? "overwhelming" : ratio >= 1.4 ? "stronger than you" : ratio >= 0.8 ? "about your equal" : "weaker than you";
        String intent = mood == Mood.HOSTILE ? "and means to fight" : mood == Mood.DUELIST ? "and is waiting for a challenge" : "and seems friendly";
        sp.displayClientMessage(Component.empty().append(getDisplayName())
                .append(Component.literal("  " + cmp + " (" + myPow + ") " + intent).withStyle(ChatFormatting.GRAY)), true);
        if (rank().ordinal() >= Rank.ELITE.ordinal() && level() instanceof ServerLevel sl)
            sl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private void aura(ServerLevel sl, Rank r) {
        Vector3f c = new Vector3f(((r.color >> 16) & 255) / 255F, ((r.color >> 8) & 255) / 255F, (r.color & 255) / 255F);
        sl.sendParticles(new DustParticleOptions(c, 1.3F), getX(), getY() + 1.0, getZ(), 3 + r.ordinal(), 0.45, 0.9, 0.45, 0.01);
        if (r == Rank.LEGEND && tickCount % 12 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 2, 0.5, 1.0, 0.5, 0.02);
    }

    private void fightExtras(ServerLevel sl, ServerPlayer t, Rank r) {
        double d = distanceTo(t);
        if (r.ordinal() >= Rank.ELITE.ordinal() && flickerCd <= 0 && d > 9) { flicker(sl, t); flickerCd = 140 - r.ordinal() * 10 + random.nextInt(60); return; }
        if (r.ordinal() >= Rank.JONIN.ordinal() && jutsuCd <= 0 && d >= 5 && d <= 22 && hasLineOfSight(t)) {
            windup = 16;
            jutsuCd = (r == Rank.JONIN ? 150 : r == Rank.ELITE ? 110 : 90) + random.nextInt(50);
            getNavigation().stop();
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 1.0F);
        }
    }

    private void flicker(ServerLevel sl, ServerPlayer t) {
        Vec3 look = t.getLookAngle();
        Vec3 behind = t.position().add(-look.x * 2.5, 0, -look.z * 2.5);
        Vec3 delta = behind.subtract(position());
        if (!level().noCollision(this, getBoundingBox().move(delta))) return;
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        moveTo(behind.x, behind.y, behind.z, getYRot(), getXRot());
        getNavigation().stop();
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.2F);
    }

    private void windup(ServerLevel sl, LivingEntity tg) {
        windup--;
        getNavigation().stop();
        if (tg != null) getLookControl().setLookAt(tg, 60F, 60F);
        Rank r = rank();
        Vector3f c = new Vector3f(((r.color >> 16) & 255) / 255F, ((r.color >> 8) & 255) / 255F, (r.color & 255) / 255F);
        sl.sendParticles(new DustParticleOptions(c, 1.5F), getX(), getY() + 1.3, getZ(), 6, 0.35, 0.35, 0.35, 0.02);
        if (windup == 0 && tg instanceof ServerPlayer sp && sp.isAlive()) cast(sl, sp, r);
    }

    private void cast(ServerLevel sl, ServerPlayer t, Rank r) {
        Vec3 from = getEyePosition(), to = t.getEyePosition();
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.01) return;
        Vec3 n = dir.scale(1.0 / len);
        ParticleOptions trail = r == Rank.JONIN ? ParticleTypes.CLOUD : r == Rank.ELITE ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME;
        for (double s = 1.0; s < len; s += 0.8) sl.sendParticles(trail, from.x + n.x * s, from.y + n.y * s, from.z + n.z * s, 1, 0.05, 0.05, 0.05, 0.0);
        float lvl = MobLevelHandler.getLevel(this);
        if (r == Rank.LEGEND) {
            float dmg = 5F + lvl * 0.15F;
            sl.sendParticles(ParticleTypes.FLAME, t.getX(), t.getY() + 1, t.getZ(), 60, 1.2, 0.8, 1.2, 0.05);
            sl.sendParticles(ParticleTypes.EXPLOSION, t.getX(), t.getY() + 1, t.getZ(), 2, 0.4, 0.4, 0.4, 0.0);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(3.5),
                    x -> x != this && !(x instanceof WandererEntity) && !x.getTags().contains(FRIENDLY_TAG))) {
                e.hurt(damageSources().mobAttack(this), dmg);
                e.setSecondsOnFire(4);
            }
            sl.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.2F);
        } else {
            float dmg = r == Rank.JONIN ? 4F + lvl * 0.12F : 5F + lvl * 0.16F;
            t.hurt(damageSources().mobAttack(this), dmg);
            t.knockback(r == Rank.JONIN ? 0.9 : 0.5, -n.x, -n.z);
            if (r == Rank.ELITE) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
            sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + 1, t.getZ(), 14, 0.4, 0.5, 0.4, 0.1);
            sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    // ------------------------------------------------------------------------------------------------ save / load

    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("wRank", entityData.get(RANK));
        t.putInt("wMood", mood.ordinal());
        t.putBoolean("wDuel", duel);
        t.putBoolean("wYield", yielded);
        t.putBoolean("wCfg", configured);
        ListTag l = new ListTag();
        for (UUID u : beaten) l.add(StringTag.valueOf(u.toString()));
        t.put("wBeaten", l);
    }

    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(RANK, Math.max(0, Math.min(Rank.values().length - 1, t.getInt("wRank"))));
        mood = Mood.values()[Math.max(0, Math.min(Mood.values().length - 1, t.getInt("wMood")))];
        duel = t.getBoolean("wDuel");
        yielded = t.getBoolean("wYield");
        configured = t.getBoolean("wCfg");
        beaten.clear();
        ListTag l = t.getList("wBeaten", 8);
        for (int i = 0; i < l.size(); i++) {
            try { beaten.add(UUID.fromString(l.getString(i))); } catch (IllegalArgumentException ignored) {}
        }
        getPersistentData().putBoolean(EnemyCombat.TAG_ROGUE, true);
        addTag(EnemyCombat.TAG_ROGUE);
        if (rank().ordinal() >= Rank.ELITE.ordinal() && bar == null) makeBar();
    }
}
