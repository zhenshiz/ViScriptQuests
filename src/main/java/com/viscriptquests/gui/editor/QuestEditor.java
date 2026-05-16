package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.SplittableWindow;
import com.viscriptquests.ViScriptQuests;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class QuestEditor extends Editor {
    public static final ResourceLocation EDITOR_ID = ViScriptQuests.id("quest_editor");

    public QuestEditor() {
        this.leftWindow.setDisplay(TaffyDisplay.NONE);
        this.leftWindow.getParentWindow().removeSplitWindow(this.leftWindow);
        this.bottomWindow.setDisplay(TaffyDisplay.NONE);
        this.bottomWindow.getParentWindow().removeSplitWindow(this.bottomWindow);
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

    // 移除空白面板：先隐藏再从父窗口中分离，让中央蓝图占满整个编辑区域
    private void removeEmptyPanel(SplittableWindow window) {
        window.setDisplay(false);
        var parent = window.getParentWindow();
        if (parent != null) {
            parent.removeSplitWindow(window);
        }
    }
}
