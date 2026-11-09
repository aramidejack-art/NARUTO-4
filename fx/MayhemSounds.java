package com.mayhem.fx;

import com.mayhem.MayhemMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Custom synthesized sound effects (see assets/mayhem/sounds). */
public final class MayhemSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MayhemMod.MOD_ID);

    public static final RegistryObject<SoundEvent> WHOOSH_LIGHT = reg("whoosh_light");
    public static final RegistryObject<SoundEvent> WHOOSH_HEAVY = reg("whoosh_heavy");
    public static final RegistryObject<SoundEvent> HIT_LIGHT = reg("hit_light");
    public static final RegistryObject<SoundEvent> BARRAGE_HIT = reg("barrage_hit");
    public static final RegistryObject<SoundEvent> HIT_HEAVY = reg("hit_heavy");
    public static final RegistryObject<SoundEvent> BOOM = reg("boom");
    public static final RegistryObject<SoundEvent> FLASH_STEP = reg("flash_step");
    public static final RegistryObject<SoundEvent> CHARGE = reg("charge");
    public static final RegistryObject<SoundEvent> PARRY = reg("parry");
    public static final RegistryObject<SoundEvent> IMPACT_FRAME = reg("impact_frame");

    private MayhemSounds() {}

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(MayhemMod.MOD_ID, name)));
    }
}
