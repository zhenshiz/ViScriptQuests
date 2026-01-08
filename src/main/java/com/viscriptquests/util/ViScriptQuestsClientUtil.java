package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.viscriptquests.gui.QuestBookUI;
import com.viscriptquests.gui.QuestEditor;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@KJSBindings(value = "client", modId = "viscript_quests", clientOnly = true)
public class ViScriptQuestsClientUtil {
    private static final Minecraft minecraft = Minecraft.getInstance();

    @Info("客户端打开任务书")
    public static void openQuestBook() {
        QuestBookUI questBookUI = new QuestBookUI();
        ModularUI ui = ModularUI.of(UI.of(questBookUI));
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }

    @Info("客户端打开任务编辑器")
    public static void openQuestEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        EditorWindow editorWindow = EditorWindow.open(QuestEditor.QUEST_ID, QuestEditor::new);
        ModularUI ui = new ModularUI(UI.of(editorWindow));
        if (!Platform.isDevEnv()) ui.shouldCloseOnEsc(false).shouldCloseOnKeyInventory(false);
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }
}
