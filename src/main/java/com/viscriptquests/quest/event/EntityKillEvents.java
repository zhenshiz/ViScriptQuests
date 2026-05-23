package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class EntityKillEvents {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        QuestSubmissionService.recordEntityDeath(event.getEntity());
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            QuestSubmissionService.recordEntityKill(player, event.getEntity());
        }
    }
}
