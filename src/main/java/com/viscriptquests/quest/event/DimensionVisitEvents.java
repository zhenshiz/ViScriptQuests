package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.task.VisitDimensionTask;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class DimensionVisitEvents {
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            QuestSubmissionService.submitActiveTasks(player, VisitDimensionTask.class);
        }
    }
}
