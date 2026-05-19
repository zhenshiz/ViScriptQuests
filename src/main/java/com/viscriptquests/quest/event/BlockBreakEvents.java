package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
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
