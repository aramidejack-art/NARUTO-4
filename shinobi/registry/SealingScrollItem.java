package com.mayhem.shinobi.registry;

import com.mayhem.shinobi.beast.BeastBond;
import com.mayhem.shinobi.beast.BeastEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Used on a kneeling tailed beast to begin the sealing ritual. */
public class SealingScrollItem extends Item {
    public SealingScrollItem(Properties props) { super(props); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof BeastEntity beast) {
            if (!player.level().isClientSide && player instanceof ServerPlayer sp) BeastBond.startRitual(sp, beast, stack);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
