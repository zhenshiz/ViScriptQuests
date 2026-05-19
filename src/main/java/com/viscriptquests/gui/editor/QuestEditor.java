package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.SplittableWindow;
import com.viscriptquests.ViScriptQuests;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class QuestEditor extends Editor {
    public static final ResourceLocation EDITOR_ID = ViScriptQuests.id("quest_editor");

    public QuestEditor() {
        removeEmptyPanel(leftWindow);
        removeEmptyPanel(bottomWindow);

        var centerViewContainer = centerWindow.getViewContainer();
        if (centerViewContainer != null) {
            rootWindow.setViewContainer(centerViewContainer);
        }

        centerWindow.setDisplay(false);
        rightWindow.setDisplay(false);
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

    // 移除非根面板：先隐藏再从父窗口中分离，让剩余面板接管原本的分屏空间。
    private void removeEmptyPanel(SplittableWindow window) {
        window.setDisplay(false);
        var parent = window.getParentWindow();
        if (parent != null) {
            parent.removeSplitWindow(window);
        }
    }
}
