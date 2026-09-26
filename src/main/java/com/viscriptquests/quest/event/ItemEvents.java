package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestManager;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class ItemEvents {
    // 每次检查的事件间隔（单位：tick）
    private static final int TRACKED_TASK_CHECK_INTERVAL_TICKS = 19;
    private static final int COUNTDOWN_CHECK_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START || !(event.player instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime % COUNTDOWN_CHECK_INTERVAL_TICKS == 0) {
            QuestSubmissionService.tickCountdownTasks(player);
        }
        if (gameTime % TRACKED_TASK_CHECK_INTERVAL_TICKS == 0) {
            QuestManager.submitTracked(player);
        }
    }
}
