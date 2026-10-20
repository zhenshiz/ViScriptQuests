package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.QuestBookUI;
import com.viscriptquests.gui.QuestCategoryConfigUI;
import com.viscriptquests.gui.editor.QuestEditor;
import com.viscriptquests.gui.editor.QuestProject;
import com.viscriptquests.quest.data.runtime.QuestBookData;
import com.viscriptquests.quest.data.runtime.QuestCategoryConfigData;
import com.viscriptquests.quest.data.runtime.QuestPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class ViScriptQuestsClientUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();

    // Screen 和 Minecraft 的调用只保留在客户端类中，避免服务端扫描 RPC 时加载客户端类型。
    public static void openEditorWithProject(CompoundTag graphTag) {
        QuestProject project = QuestProject.createProject(graphTag);
        EditorWindow editorWindow = getCurrentEditorWindow();
        if (editorWindow == null) {
            editorWindow = QuestEditor.openWindow();
            minecraft.setScreen(new ModularUIScreen(
                    new ModularUI(UI.of(editorWindow)).shouldCloseOnKeyInventory(false),
                    Component.translatable("screen.viscript_quests.quest_editor")));
        }
        var editor = editorWindow.getCurrentEditor();
        if (editor == null) return;
        try {
            editor.loadProject(project, null);
        } catch (Exception exception) {
            ViScriptQuests.LOGGER.error("无法打开服务端任务工程", exception);
        }
    }

    private static EditorWindow getCurrentEditorWindow() {
        if (minecraft.screen instanceof ModularUIContainerScreen screen
                && screen.getMenu().getModularUI().ui.rootElement instanceof EditorWindow editorWindow) {
            return editorWindow;
        }
        if (minecraft.screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof EditorWindow editorWindow) {
            return editorWindow;
        }
        return null;
    }

    // 打开调试任务书 UI：反序列化任务数据后创建 QuestBookUI
    public static void openQuestBook(CompoundTag data) {
        QuestBookData bookData = new QuestBookData();
        bookData.deserializeNBT(Platform.getFrozenRegistry(), data);
        QuestBookUI questBookUI = new QuestBookUI(bookData);
        ModularUI modularUI = new ModularUI(UI.of(questBookUI, QuestBookUI::getViewportSize));
        minecraft.setScreen(new ModularUIScreen(modularUI,
                Component.translatable("screen.viscript_quests.quest_book")));
    }

    public static void syncQuestBook(CompoundTag data) {
        QuestBookData bookData = new QuestBookData();
        bookData.deserializeNBT(Platform.getFrozenRegistry(), data);
        if (minecraft.screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof QuestBookUI questBookUI) {
            questBookUI.syncBookData(bookData);
        }
    }

    public static void syncQuestBookPlayerData(CompoundTag data) {
        if (minecraft.screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof QuestBookUI questBookUI) {
            QuestPlayerData playerData = new QuestPlayerData();
            playerData.deserializeNBT(Platform.getFrozenRegistry(), data);
            questBookUI.syncPlayerData(playerData);
        }
    }

    public static void openCategoryConfig(CompoundTag data) {
        QuestCategoryConfigData configData = new QuestCategoryConfigData();
        configData.deserializeNBT(Platform.getFrozenRegistry(), data);
        QuestCategoryConfigUI categoryConfigUI = new QuestCategoryConfigUI(configData);
        ModularUI modularUI = new ModularUI(UI.of(categoryConfigUI, QuestCategoryConfigUI::getAutoGuiScaledSize));
        minecraft.setScreen(new ModularUIScreen(modularUI,
                Component.translatable("screen.viscript_quests.category_config")));
    }
}
