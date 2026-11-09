package com.mayhem.shinobi.registry;

import com.mayhem.MayhemMod;
import com.mayhem.shinobi.beast.BeastEntity;
import com.mayhem.shinobi.npc.NpcEntity;
import com.mayhem.shinobi.npc.TrainingDummyEntity;
import com.mayhem.shinobi.npc.EscortClientEntity;
import com.mayhem.shinobi.npc.RogueNinjaEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Entities, blocks and items added by the shinobi systems. */
public final class ShinobiRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MayhemMod.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MayhemMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MayhemMod.MOD_ID);

    public static final RegistryObject<Block> CHAKRA_SURFACE = BLOCKS.register("chakra_surface", ChakraSurfaceBlock::new);
    public static final RegistryObject<Block> MISSION_BOARD = BLOCKS.register("mission_board", MissionBoardBlock::new);

    public static final RegistryObject<EntityType<BeastEntity>> BEAST = ENTITIES.register("tailed_beast",
            () -> EntityType.Builder.of(BeastEntity::new, MobCategory.MONSTER).sized(3.0F, 4.0F).clientTrackingRange(16).fireImmune().build("tailed_beast"));
    public static final RegistryObject<EntityType<NpcEntity>> NPC = ENTITIES.register("shinobi_npc",
            () -> EntityType.Builder.of(NpcEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("shinobi_npc"));
    public static final RegistryObject<EntityType<TrainingDummyEntity>> TRAINING_DUMMY = ENTITIES.register("training_dummy",
            () -> EntityType.Builder.of(TrainingDummyEntity::new, MobCategory.MISC).sized(0.7F, 1.9F).clientTrackingRange(8).build("training_dummy"));
    public static final RegistryObject<EntityType<EscortClientEntity>> ESCORT_CLIENT = ENTITIES.register("escort_client",
            () -> EntityType.Builder.of(EscortClientEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(10).build("escort_client"));
    public static final RegistryObject<EntityType<RogueNinjaEntity>> ROGUE_NINJA = ENTITIES.register("rogue_ninja",
            () -> EntityType.Builder.of(RogueNinjaEntity::new, MobCategory.MONSTER).sized(0.6F, 1.85F).clientTrackingRange(10).build("rogue_ninja"));

    public static final RegistryObject<EntityType<com.mayhem.shinobi.npc.WandererEntity>> WANDERER = ENTITIES.register("wanderer",
            () -> EntityType.Builder.<com.mayhem.shinobi.npc.WandererEntity>of(com.mayhem.shinobi.npc.WandererEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("wanderer"));

    public static final RegistryObject<EntityType<com.mayhem.shinobi.avatar.ChakraAvatarEntity>> AVATAR = ENTITIES.register("chakra_avatar",
            () -> EntityType.Builder.<com.mayhem.shinobi.avatar.ChakraAvatarEntity>of(com.mayhem.shinobi.avatar.ChakraAvatarEntity::new, MobCategory.MISC).sized(0.2F, 0.2F).clientTrackingRange(16).updateInterval(1).fireImmune().noSave().build("chakra_avatar"));

    public static final RegistryObject<EntityType<com.mayhem.shinobi.fx.JutsuFxEntity>> JUTSU_FX = ENTITIES.register("jutsu_fx",
            () -> EntityType.Builder.<com.mayhem.shinobi.fx.JutsuFxEntity>of(com.mayhem.shinobi.fx.JutsuFxEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(12).updateInterval(1).fireImmune().noSave().build("jutsu_fx"));

    public static final RegistryObject<Item> SEALING_SCROLL = ITEMS.register("sealing_scroll", () -> new SealingScrollItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> NPC_EGG = ITEMS.register("shinobi_npc_spawn_egg", () -> new ForgeSpawnEggItem(NPC, 0x2B3A55, 0xE6C27A, new Item.Properties()));
    public static final RegistryObject<Item> WANDERER_EGG = ITEMS.register("wanderer_spawn_egg", () -> new ForgeSpawnEggItem(WANDERER, 0x23283A, 0xD7B25A, new Item.Properties()));
    public static final RegistryObject<Item> BEAST_EGG = ITEMS.register("tailed_beast_spawn_egg", () -> new ForgeSpawnEggItem(BEAST, 0x5A1F1F, 0xFF7A00, new Item.Properties()));
    public static final RegistryObject<Item> ROGUE_EGG = ITEMS.register("rogue_ninja_spawn_egg", () -> new ForgeSpawnEggItem(ROGUE_NINJA, 0x1A1A2E, 0xE94560, new Item.Properties()));
    public static final RegistryObject<Item> DUMMY_EGG = ITEMS.register("training_dummy_spawn_egg", () -> new ForgeSpawnEggItem(TRAINING_DUMMY, 0x8B7355, 0xD2B48C, new Item.Properties()));
    public static final RegistryObject<Item> MISSION_BOARD_ITEM = ITEMS.register("mission_board", () -> new BlockItem(MISSION_BOARD.get(), new Item.Properties()));

    private ShinobiRegistry() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.register(new ModBusEvents());
    }

    public static final class ModBusEvents {
        @SubscribeEvent
        public void attributes(EntityAttributeCreationEvent e) {
            e.put(BEAST.get(), BeastEntity.createAttributes().build());
            e.put(NPC.get(), NpcEntity.createAttributes().build());
            e.put(ROGUE_NINJA.get(), RogueNinjaEntity.createAttributes().build());
            e.put(WANDERER.get(), com.mayhem.shinobi.npc.WandererEntity.createAttributes().build());
            e.put(TRAINING_DUMMY.get(), TrainingDummyEntity.createAttributes().build());
            e.put(ESCORT_CLIENT.get(), EscortClientEntity.createAttributes().build());
        }

        @SubscribeEvent
        public void tabs(BuildCreativeModeTabContentsEvent e) {
            if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                e.accept(NPC_EGG);
                e.accept(BEAST_EGG);
                e.accept(ROGUE_EGG);
                e.accept(WANDERER_EGG);
                e.accept(DUMMY_EGG);
            }
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(SEALING_SCROLL);
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(MISSION_BOARD_ITEM);
        }
    }
}
