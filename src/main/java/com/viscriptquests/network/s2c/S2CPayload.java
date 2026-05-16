package com.viscriptquests.network.s2c;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.editor.QuestEditor;
import com.viscriptquests.gui.editor.QuestProject;
import com.viscriptquests.util.ViScriptQuestsClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class S2CPayload {
    public static final String OPEN_EDITOR_WITH_PROJECT = ViScriptQuests.MOD_ID + ":open_editor_with_project";
    public static final String OPEN_QUEST_BOOK = ViScriptQuests.MOD_ID + ":open_quest_book";
    public static final String OPEN_CATEGORY_CONFIG = ViScriptQuests.MOD_ID + ":open_category_config";

    // 服务端发送项目图数据到客户端，打开编辑器并加载该图
    @RPCPacket(OPEN_EDITOR_WITH_PROJECT)
    public static void openEditorWithProject(CompoundTag graphTag) {
        QuestProject project = QuestProject.createProject(graphTag);
        EditorWindow editorWindow = getCurrentEditorWindow();
        if (editorWindow == null) return;

        Editor editor = editorWindow.getCurrentEditor();
        if (editor == null) return;
        try {
            editor.loadProject(project, null);
        } catch (Exception ignored) {
        }
    }

    private static EditorWindow getCurrentEditorWindow() {
        if (Minecraft.getInstance().screen instanceof ModularUIContainerScreen screen
                && screen.getMenu().getModularUI().ui.rootElement instanceof EditorWindow editorWindow) {
            return editorWindow;
        }
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof EditorWindow editorWindow) {
            return editorWindow;
        }
        return null;
    }

    @RPCPacket(OPEN_QUEST_BOOK)
    public static void openQuestBook(CompoundTag data) {
        if (LDLib2.isClient()) {
            ViScriptQuestsClientUtil.openQuestBook(data);
        }
    }

    @RPCPacket(OPEN_CATEGORY_CONFIG)
    public static void openCategoryConfig(CompoundTag data) {
        if (LDLib2.isClient()) {
            ViScriptQuestsClientUtil.openCategoryConfig(data);
        }
    }
}
