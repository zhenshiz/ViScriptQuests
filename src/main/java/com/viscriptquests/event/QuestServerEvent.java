package com.viscriptquests.event;

import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.QuestSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = ViScriptQuests.MOD_ID)
public class QuestServerEvent {
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            ViScriptQuests.setQuestSavedData(world.getDataStorage().computeIfAbsent(QuestSavedData.factory(world), "category_quests"));
        }
    }
}
