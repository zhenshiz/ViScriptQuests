package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorLayout;
import com.lowdragmc.lowdraglib2.editor.ui.ViewContainer;
import com.viscriptquests.ViScriptQuests;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class QuestEditor extends Editor {
    public static final ResourceLocation EDITOR_ID = ViScriptQuests.id("quest_editor");

    public QuestEditor() {
        rootWindow.setViewContainer(new ViewContainer());
    }

    @Override
    protected void initMenus() {
        super.initMenus();
        fileMenu.addProjectProvider(QuestProject.TYPE);
    }

    @Override
    protected @Nonnull Editor createNewEditorInstance() {
        return new QuestEditor();
    }

    @Override
    public void applyLayout(EditorLayout layout) {
        // 任务编辑器固定为单窗口蓝图编辑器，不恢复 LDLib2 默认的资源/历史/检查器多面板布局。
    }
}
