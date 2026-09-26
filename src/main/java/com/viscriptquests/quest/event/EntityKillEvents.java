package com.viscriptquests.quest.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.runtime.QuestSubmissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
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
