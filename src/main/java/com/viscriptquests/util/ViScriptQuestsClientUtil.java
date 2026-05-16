package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.QuestBookUI;
import com.viscriptquests.gui.QuestCategoryConfigUI;
import com.viscriptquests.gui.editor.QuestEditor;
import com.viscriptquests.gui.editor.QuestProject;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

@KJSBindings(value = "ViScriptQuestsUtil", clientOnly = true)
public class ViScriptQuestsClientUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();

    // 打开调试任务书 UI：反序列化任务数据后创建 QuestBookUI
    public static void openQuestBook(CompoundTag data) {
        QuestPlayerData playerData = new QuestPlayerData();
        playerData.deserializeNBT(Platform.getFrozenRegistry(), data);
        QuestBookUI questBookUI = new QuestBookUI(playerData);
        ModularUI modularUI = new ModularUI(UI.of(questBookUI, QuestBookUI::getAutoGuiScaledSize));
        minecraft.setScreen(new ModularUIScreen(modularUI,
                Component.translatable("screen.viscript_quests.quest_book")));
    }

    public static void openCategoryConfig(CompoundTag data) {
        QuestCategoryListData categoryData = new QuestCategoryListData();
        categoryData.deserializeNBT(Platform.getFrozenRegistry(), data);
        QuestCategoryConfigUI categoryConfigUI = new QuestCategoryConfigUI(categoryData);
        ModularUI modularUI = new ModularUI(UI.of(categoryConfigUI, QuestCategoryConfigUI::getAutoGuiScaledSize));
        minecraft.setScreen(new ModularUIScreen(modularUI,
                Component.translatable("screen.viscript_quests.category_config")));
    }
}
