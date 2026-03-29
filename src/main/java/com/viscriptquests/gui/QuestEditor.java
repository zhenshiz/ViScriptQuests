package com.viscriptquests.gui;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.project.QuestProject;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class QuestEditor extends Editor {
    public final static ResourceLocation QUEST_ID = ViScriptQuests.id("editor");


    public QuestEditor() {
        this.fileMenu.addProjectProvider(QuestProject.PROVIDER);
        this.bottomWindow.setDisplay(TaffyDisplay.NONE);
        this.bottomWindow.getParentWindow().removeSplitWindow(this.bottomWindow);
    }

    @Override
    protected Editor createNewEditorInstance() {
        return new QuestEditor();
    }

    @Override
    protected void loadNewProject(IProject project, @Nullable File projectFile) {
        if (project instanceof QuestProject questProject) {
            super.loadNewProject(project, projectFile);
        }
    }
}
