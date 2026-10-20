package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestManager;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import com.viscriptquests.quest.runtime.QuestTrackingService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class ItemEvents {
    // 每次检查的事件间隔（单位：tick）
    private static final int TRACKED_TASK_CHECK_INTERVAL_TICKS = 19;
    private static final int COUNTDOWN_CHECK_INTERVAL_TICKS = 20;

    /**
     * 在服务端记录完整使用物品的事件，由食物目标筛选使用前的食物、饮品和手部。
     *
     * @param event 物品使用完成事件，包含使用前物品栈及使用的手
     */
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestSubmissionService.recordFoodEaten(player, event.getItem(), event.getHand());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
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
