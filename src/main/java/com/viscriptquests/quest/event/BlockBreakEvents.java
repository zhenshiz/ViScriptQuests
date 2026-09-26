package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class BlockBreakEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof Level level)
                || level.isClientSide()
                || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        QuestSubmissionService.recordBlockBreak(player, event.getState());
    }
}
