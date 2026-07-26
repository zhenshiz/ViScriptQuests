package com.viscriptquests.gui.editor;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.viscript_lib.gui.editor.IRuntimeFileProject;
import com.viscript_lib.gui.editor.ProjectFileProjectType;
import com.viscriptquests.gui.blueprint.compiler.QuestBlueprintCompiler;
import com.viscriptquests.gui.blueprint.QuestBlueprintExamples;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintNodeLibrary;
import com.viscriptquests.quest.data.QuestFile;
import com.viscriptquests.util.QuestFileHelper;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class QuestProject implements IRuntimeFileProject {
    public static final ProjectFileProjectType TYPE = new QuestProjectType();

    @Getter
    private final Resources resources = Resources.EMPTY;
    private CompoundTag graphTag = new CompoundTag();
    @Nullable
    private transient GraphEditorView graphEditorView;
    @Nullable
    private transient Supplier<CompoundTag> graphSnapshotSupplier;

    public static QuestProject createProject(CompoundTag graphTag) {
        QuestProject project = new QuestProject();
        project.graphTag = graphTag.copy();
        return project;
    }

    @Override
    public ProjectFileProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public void initNewProject() {
        graphTag = QuestBlueprintExamples.createSimpleQuestGraphTag();
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        refreshGraphSnapshot();
        CompoundTag tag = new CompoundTag();
        tag.put("graph", graphTag.copy());
        return tag;
    }

    @Override
    public CompoundTag serializeRuntimeFile(@NotNull HolderLookup.Provider provider) {
        return compileQuestFile().serializeNBT(provider);
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        graphTag = nbt.getCompound("graph").copy();
    }

    @Override
    public void onLoad(Editor editor) {
        QuestBlueprintGraph graph = new QuestBlueprintGraph();
        if (!graphTag.isEmpty()) {
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), graphTag.copy());
        }

        graphEditorView = new GraphEditorView().loadGraph(graph, savedGraph -> graphTag = savedGraph.copy());
        graphSnapshotSupplier = graphEditorView::serializeGraph;
        graphEditorView.setCanRemove(false);
        graphEditorView.setIcon(Icons.NODE);
        graphEditorView.setDynamicName(() -> Component.translatable("viscript_quests.editor.view.quest_blueprint"));

        editor.placeView(graphEditorView, () -> editor.centerWindow.getLeftTop());
        QuestBlueprintNodeLibrary.rebuild(graphEditorView.graphView.itemLibrary, graph.graphModel);
    }

    @Override
    public void onClosed(Editor editor) {
        refreshGraphSnapshot();
        editor.inspectorView.clear();
        if (graphEditorView != null) {
            graphEditorView.clear();
            graphEditorView.removeSelf();
        }
        graphEditorView = null;
        graphSnapshotSupplier = null;
    }

    public QuestFile compileQuestFile() {
        refreshGraphSnapshot();
        return QuestBlueprintCompiler.compile(graphTag);
    }

    public CompoundTag currentGraphTag() {
        refreshGraphSnapshot();
        return graphTag.copy();
    }

    private void refreshGraphSnapshot() {
        if (graphSnapshotSupplier != null) {
            graphTag = graphSnapshotSupplier.get().copy();
        }
    }

    private static class QuestProjectType extends ProjectFileProjectType {
        private QuestProjectType() {
            super(Icons.NODE, "viscript_quests.editor.quest.add", QuestFileHelper.FORMAT, QuestProject::new);
        }
    }
}
