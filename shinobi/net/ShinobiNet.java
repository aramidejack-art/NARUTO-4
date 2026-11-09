package com.mayhem.shinobi.net;

import com.mayhem.network.MayhemNetwork;
import com.mayhem.shinobi.ShinobiActions;
import com.mayhem.shinobi.client.ClientShinobi;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Shinobi packets (registered on the shared MAYHEM channel). Client handlers live in ClientShinobi. */
public final class ShinobiNet {
    private ShinobiNet() {}

    public enum Op {
        SELECT_ELEMENT, ADD_ELEMENT, SELECT_CLAN, SELECT_BLOODLINE, LEARN, ASSIGN, CLEAR_SLOT, LOAD_PRESET, SAVE_PRESET,
        SWITCH_STYLE, CAST, SET_VARIANT, SPEND_STAT, ACCEPT_MISSION, ABANDON_MISSION, CLAIM_MISSION, TOGGLE_MODE,
        TOGGLE_CLOAK, CHARGE_ON, CHARGE_OFF, CHALLENGE_BEAST, DIALOGUE, AWAKEN, AWAKEN_STAGE, ONBOARD_DONE, REQUEST_SYNC,
        SELECT_BEAST_INFO, GUARDIAN_TOGGLE, GUARDIAN_STAGE, AVATAR_STANCE, AVATAR_PUNCH, AVATAR_TACKLE, BEAST_MODE, MODE_SKILL
    }

    private static int id = 100;

    public static void register(SimpleChannel ch) {
        ch.messageBuilder(Action.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(Action::encode).decoder(Action::decode).consumerMainThread(Action::handle).add();
        ch.messageBuilder(AppearSet.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(AppearSet::encode).decoder(AppearSet::decode).consumerMainThread(AppearSet::handle).add();
        ch.messageBuilder(Sync.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Sync::encode).decoder(Sync::decode).consumerMainThread(Sync::handle).add();
        ch.messageBuilder(Res.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Res::encode).decoder(Res::decode).consumerMainThread(Res::handle).add();
        ch.messageBuilder(Ack.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Ack::encode).decoder(Ack::decode).consumerMainThread(Ack::handle).add();
        ch.messageBuilder(Banner.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Banner::encode).decoder(Banner::decode).consumerMainThread(Banner::handle).add();
        ch.messageBuilder(Dialogue.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(Dialogue::encode).decoder(Dialogue::decode).consumerMainThread(Dialogue::handle).add();
        ch.messageBuilder(AppearSync.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(AppearSync::encode).decoder(AppearSync::decode).consumerMainThread(AppearSync::handle).add();
    }

    // ------------------------------------------------------------------ server -> client helpers
    public static void syncTag(ServerPlayer p) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Sync(ShinobiData.of(p).copy()));
    }

    public static void sendResources(ServerPlayer p, boolean force) {
        CompoundTag t = ShinobiData.of(p);
        ShinobiState st = ShinobiState.of(p.getUUID());
        float c = Resources.chakra(t), s = Resources.stamina(t);
        int flags = (st.charging ? 1 : 0) | (t.getBoolean("waterWalk") ? 2 : 0) | (t.getBoolean("wallWalk") ? 4 : 0)
                | (ShinobiData.cloak(t) ? 8 : 0) | (ShinobiData.style(t) == com.mayhem.shinobi.data.Style.NINJUTSU ? 16 : 0)
                | (t.getInt("awakenTicks") > 0 ? 32 : 0);
        String mode = ShinobiData.mode(t);
        if (!force && Math.abs(c - st.sentChakra) < 0.5F && Math.abs(s - st.sentStamina) < 0.5F && flags == st.sentFlags && mode.equals(st.sentMode)) return;
        st.sentChakra = c; st.sentStamina = s; st.sentFlags = flags; st.sentMode = mode;
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new Res(c, Resources.maxChakra(p, t), s, Resources.maxStamina(p, t), flags, mode, t.getInt("awakenTicks")));
    }

    public static void ack(ServerPlayer p, String abilityId, int cd) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Ack(abilityId, cd));
    }

    /** kind: 0 info banner, 1 story, 2 event, 3 level/unlock */
    public static void banner(ServerPlayer p, int kind, String title, String sub) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Banner((byte) kind, title, sub));
    }

    public static void dialogue(ServerPlayer p, int entityId, String name, String text, List<String> options) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Dialogue(entityId, name, text, options));
    }

    public static void appearanceTo(ServerPlayer target, UUID who, CompoundTag tag) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new AppearSync(who, tag));
    }

    public static void appearanceToTrackers(ServerPlayer who) {
        CompoundTag tag = ShinobiData.appearance(ShinobiData.of(who)).copy();
        MayhemNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> who), new AppearSync(who.getUUID(), tag));
    }

    // ------------------------------------------------------------------ packets
    public static final class Action {
        final Op op; final String s; final int a, b;
        public Action(Op op, String s, int a, int b) { this.op = op; this.s = s == null ? "" : s; this.a = a; this.b = b; }
        public static void encode(Action p, FriendlyByteBuf buf) { buf.writeEnum(p.op); buf.writeUtf(p.s, 128); buf.writeVarInt(p.a); buf.writeVarInt(p.b); }
        public static Action decode(FriendlyByteBuf buf) { return new Action(buf.readEnum(Op.class), buf.readUtf(128), buf.readVarInt(), buf.readVarInt()); }
        public static void handle(Action p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            ServerPlayer sp = c.getSender();
            if (sp != null) ShinobiActions.handle(sp, p.op, p.s, p.a, p.b);
            c.setPacketHandled(true);
        }
    }

    public static final class AppearSet {
        final CompoundTag tag;
        public AppearSet(CompoundTag tag) { this.tag = tag; }
        public static void encode(AppearSet p, FriendlyByteBuf buf) { buf.writeNbt(p.tag); }
        public static AppearSet decode(FriendlyByteBuf buf) { CompoundTag t = buf.readNbt(); return new AppearSet(t == null ? new CompoundTag() : t); }
        public static void handle(AppearSet p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            ServerPlayer sp = c.getSender();
            if (sp != null) com.mayhem.shinobi.appearance.Appearance.apply(sp, p.tag);
            c.setPacketHandled(true);
        }
    }

    public static final class Sync {
        final CompoundTag tag;
        public Sync(CompoundTag tag) { this.tag = tag; }
        public static void encode(Sync p, FriendlyByteBuf buf) { buf.writeNbt(p.tag); }
        public static Sync decode(FriendlyByteBuf buf) { CompoundTag t = buf.readNbt(); return new Sync(t == null ? new CompoundTag() : t); }
        public static void handle(Sync p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientShinobi.sync(p.tag));
            c.setPacketHandled(true);
        }
    }

    public static final class Res {
        final float chakra, maxChakra, stamina, maxStamina; final int flags, awaken; final String mode;
        public Res(float c, float mc, float s, float ms, int flags, String mode, int awaken) {
            chakra = c; maxChakra = mc; stamina = s; maxStamina = ms; this.flags = flags; this.mode = mode; this.awaken = awaken;
        }
        public static void encode(Res p, FriendlyByteBuf b) {
            b.writeFloat(p.chakra); b.writeFloat(p.maxChakra); b.writeFloat(p.stamina); b.writeFloat(p.maxStamina);
            b.writeVarInt(p.flags); b.writeUtf(p.mode, 32); b.writeVarInt(p.awaken);
        }
        public static Res decode(FriendlyByteBuf b) {
            return new Res(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readUtf(32), b.readVarInt());
        }
        public static void handle(Res p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientShinobi.resources(p.chakra, p.maxChakra, p.stamina, p.maxStamina, p.flags, p.mode, p.awaken));
            c.setPacketHandled(true);
        }
    }

    public static final class Ack {
        final String id; final int cd;
        public Ack(String id, int cd) { this.id = id; this.cd = cd; }
        public static void encode(Ack p, FriendlyByteBuf b) { b.writeUtf(p.id, 64); b.writeVarInt(p.cd); }
        public static Ack decode(FriendlyByteBuf b) { return new Ack(b.readUtf(64), b.readVarInt()); }
        public static void handle(Ack p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientShinobi.ack(p.id, p.cd));
            c.setPacketHandled(true);
        }
    }

    public static final class Banner {
        final byte kind; final String title, sub;
        public Banner(byte kind, String title, String sub) { this.kind = kind; this.title = title; this.sub = sub; }
        public static void encode(Banner p, FriendlyByteBuf b) { b.writeByte(p.kind); b.writeUtf(p.title, 128); b.writeUtf(p.sub, 256); }
        public static Banner decode(FriendlyByteBuf b) { return new Banner(b.readByte(), b.readUtf(128), b.readUtf(256)); }
        public static void handle(Banner p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientShinobi.banner(p.kind, p.title, p.sub));
            c.setPacketHandled(true);
        }
    }

    public static final class Dialogue {
        final int entity; final String name, text; final List<String> options;
        public Dialogue(int entity, String name, String text, List<String> options) { this.entity = entity; this.name = name; this.text = text; this.options = options; }
        public static void encode(Dialogue p, FriendlyByteBuf b) {
            b.writeVarInt(p.entity); b.writeUtf(p.name, 64); b.writeUtf(p.text, 1024); b.writeVarInt(p.options.size());
            for (String o : p.options) b.writeUtf(o, 128);
        }
        public static Dialogue decode(FriendlyByteBuf b) {
            int e = b.readVarInt(); String n = b.readUtf(64), t = b.readUtf(1024); int k = Math.min(8, b.readVarInt());
            List<String> o = new ArrayList<>();
            for (int i = 0; i < k; i++) o.add(b.readUtf(128));
            return new Dialogue(e, n, t, o);
        }
        public static void handle(Dialogue p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientShinobi.dialogue(p.entity, p.name, p.text, p.options));
            c.setPacketHandled(true);
        }
    }

    public static final class AppearSync {
        final UUID who; final CompoundTag tag;
        public AppearSync(UUID who, CompoundTag tag) { this.who = who; this.tag = tag; }
        public static void encode(AppearSync p, FriendlyByteBuf b) { b.writeUUID(p.who); b.writeNbt(p.tag); }
        public static AppearSync decode(FriendlyByteBuf b) { UUID u = b.readUUID(); CompoundTag t = b.readNbt(); return new AppearSync(u, t == null ? new CompoundTag() : t); }
        public static void handle(AppearSync p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientShinobi.appearance(p.who, p.tag));
            c.setPacketHandled(true);
        }
    }
}
