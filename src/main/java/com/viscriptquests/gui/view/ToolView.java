package com.viscriptquests.gui.view;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.viscriptquests.gui.QuestEditor;
import lombok.Getter;

@Getter
public class ToolView extends View {
    private final QuestEditor questEditor;

    public ToolView(QuestEditor questEditor) {
        super("viscript_quests.editor.tool_view");
        this.questEditor = questEditor;
    }
}
