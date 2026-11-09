package com.mayhem.shinobi.registry;

import com.mayhem.shinobi.story.MissionDef;
import com.mayhem.shinobi.story.MissionTracker;
import com.mayhem.shinobi.story.Missions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Mission board block — right-click to list available contracts and accept the first eligible one.
 * Clerk NPC still works; this is a world-placeable alternative.
 */
public class MissionBoardBlock extends Block {
    public MissionBoardBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        List<MissionDef> available = new ArrayList<>();
        for (MissionDef m : Missions.ALL.values()) {
            if (m.story()) continue;
            // Offer non-story board missions the player can take
            available.add(m);
            if (available.size() >= 6) break;
        }

        sp.displayClientMessage(Component.literal("§6§l— Mission Board —"), false);
        if (available.isEmpty()) {
            sp.displayClientMessage(Component.literal("§7No contracts posted right now."), false);
            return InteractionResult.CONSUME;
        }
        int i = 1;
        for (MissionDef m : available) {
            sp.displayClientMessage(Component.literal("§e" + i + ". [" + m.rank() + "] " + m.title() + " §7— " + m.objectiveText()), false);
            i++;
        }
        // Accept first available via tracker
        MissionDef pick = available.get(0);
        if (MissionTracker.accept(sp, pick.id())) {
            sp.displayClientMessage(Component.literal("§aAccepted: " + pick.title()), false);
        } else {
            sp.displayClientMessage(Component.literal("§7Speak to the Contracts Clerk to formalize the job: §f" + pick.title()), false);
        }
        return InteractionResult.CONSUME;
    }
}
