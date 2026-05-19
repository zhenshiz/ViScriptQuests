package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.task.AdvancementTask;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class AdvancementEvents {
    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            QuestSubmissionService.recordAdvancementEarn(player, event.getAdvancement());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            // 玩家可能在接任务前已经完成过进度；登录时补一次状态型检查，避免只能靠新获得事件推进。
            QuestSubmissionService.submitActiveTasks(player, AdvancementTask.class);
        }
    }
}
