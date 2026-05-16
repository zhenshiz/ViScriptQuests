package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class ItemEvents {
    // 每次检查的事件间隔（单位：tick）
    private static final int TRACKED_TASK_CHECK_INTERVAL_TICKS = 19;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.level().getGameTime() % TRACKED_TASK_CHECK_INTERVAL_TICKS == 0) {
            QuestManager.submitTracked(player);
        }
    }
}
